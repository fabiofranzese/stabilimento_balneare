package entity;

import database.GestorePersistenza;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * RegistroPrenotazioni è il servizio di dominio per le prenotazioni (livello
 * Entity, BCED).
 *
 * Ruoli GRASP: Creator e Information Expert delle Prenotazione. Conosce l'insieme
 * delle prenotazioni, quindi gli competono il calcolo della disponibilità degli
 * ombrelloni e del residuo dei servizi, la creazione delle prenotazioni e il
 * controllo dei conflitti (estensioni 2.a e 3.1.a del flusso Effettua Prenotazione).
 *
 * Crea/aggiorna le prenotazioni e ne esegue la transizione di stato; la notifica
 * del Servizio di Notifica (Observer) NON avviene qui: è il GestoreStabilimento
 * (Controller, Subject) a notificare dopo aver ricevuto la Prenotazione restituita
 * da questi metodi, così l'interazione col sistema esterno resta nel Boundary.
 *
 * Usa GestorePersistenza (livello Database), come gli altri Registro.
 */
public class RegistroPrenotazioni {

    private GestorePersistenza gestorePersistenza;

    public RegistroPrenotazioni() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Information Expert: un ombrellone è occupato in una certa data se esiste
     * una prenotazione ATTIVA (stato Prenotata) che lo riguarda in quella data.
     * La disponibilità è quindi un dato derivato, non memorizzato sull'ombrellone.
     */
    public boolean isOmbrelloneOccupato(Ombrellone ombrellone, LocalDate data) {
        // Si cerca per ombrellone e data; l'attività si valuta sullo stato (pattern
        // State), in Java: gli stati sono ora istanze per-prenotazione, quindi non
        // si può filtrare per una riga-stato condivisa.
        Map<String, Object> criteri = new LinkedHashMap<>();
        criteri.put("ombrellone", ombrellone);
        criteri.put("data", data);

        for (Prenotazione p : gestorePersistenza.cercaPerCampi(Prenotazione.class, criteri)) {
            if (p.getStato() != null && p.getStato().isAttiva()) {
                return true;
            }
        }
        return false;
    }

    /*
     * Information Expert: indica se esiste almeno una prenotazione attiva (stato
     * Prenotata), a prescindere dalla data. Usata come precondizione dalla
     * Configurazione stabilimento, che è distruttiva e non deve essere eseguita
     * finché esistono prenotazioni che riferiscono ombrelloni e servizi.
     */
    public boolean esistonoPrenotazioniAttive() {
        for (Prenotazione p : gestorePersistenza.cercaPerCampi(Prenotazione.class, Map.of())) {
            if (p.getStato() != null && p.getStato().isAttiva()) {
                return true;
            }
        }
        return false;
    }

    /*
     * Disponibilità residua di un servizio in una data: capacità totale meno il
     * numero di prenotazioni attive (stato Prenotata) che lo includono in quella
     * data. È un dato derivato (la specifica chiede la "disponibilità limitata").
     *
     * Il conteggio si fa in Java perché i criteri generici di GestorePersistenza
     * confrontano campi semplici e non l'appartenenza a una collezione (servizi).
     */
    public int residuoServizio(ServizioAggiuntivo servizio, LocalDate data) {
        int usate = 0;

        for (Prenotazione p : prenotazioniAttive(data)) {
            usate += quantitaServizio(p, servizio);
        }

        return servizio.getCapacita() - usate;
    }

    /*
     * Quantità del servizio indicato inclusa in una prenotazione (0 se assente).
     * Il confronto è per id: gli oggetti provengono da caricamenti distinti
     * (EntityManager per-operazione) e ServizioAggiuntivo non ridefinisce equals.
     */
    private int quantitaServizio(Prenotazione prenotazione, ServizioAggiuntivo servizio) {
        if (servizio.getId() == null) {
            return 0;
        }
        for (Map.Entry<ServizioAggiuntivo, Integer> voce : prenotazione.getQuantitaServizi().entrySet()) {
            if (servizio.getId().equals(voce.getKey().getId())) {
                return voce.getValue() != null ? voce.getValue() : 0;
            }
        }
        return 0;
    }

    /*
     * Information Expert: il primo servizio la cui quantità richiesta supera il
     * residuo disponibile per la data (null se tutti hanno residuo sufficiente).
     * Espone il controllo di conflitto sui servizi (estensione 3.1.a) come query,
     * così il Controller decide l'esito senza affidarsi a eccezioni di controllo.
     */
    public ServizioAggiuntivo servizioEsaurito(Map<ServizioAggiuntivo, Integer> quantitaServizi,
                                               LocalDate data) {
        if (quantitaServizi == null) {
            return null;
        }
        for (Map.Entry<ServizioAggiuntivo, Integer> voce : quantitaServizi.entrySet()) {
            int richiesta = (voce.getValue() != null) ? voce.getValue() : 0;
            if (richiesta > 0 && residuoServizio(voce.getKey(), data) < richiesta) {
                return voce.getKey();
            }
        }
        return null;
    }

    /*
     * «Creator»: crea e salva una prenotazione nello stato Prenotata. I controlli
     * di conflitto (disponibilità ombrellone, residuo servizi) sono esposti come
     * query — isOmbrelloneOccupato, servizioEsaurito — e verificati a monte dal
     * Controller, che ne mappa l'esito in un codice: questo metodo non lancia più
     * eccezioni di dominio. Restituisce la prenotazione creata (null se il
     * salvataggio fallisce); la notifica è innescata dal Boundary alla conferma.
     */
    public Prenotazione effettuaPrenotazione(Cliente cliente, Ombrellone ombrellone,
                                             LocalDate data,
                                             Map<ServizioAggiuntivo, Integer> quantitaServizi,
                                             double prezzoTotale) {

        Map<ServizioAggiuntivo, Integer> serviziScelti =
                (quantitaServizi != null) ? quantitaServizi : new LinkedHashMap<>();

        // Pattern State: la prenotazione nasce nello stato Prenotata (istanza nuova,
        // posseduta dalla prenotazione). Il prezzo totale è "congelato" qui, alle
        // tariffe vigenti.
        Prenotazione prenotazione = new Prenotazione(
                data, ombrellone, new Prenotata(),
                cliente, new LinkedHashMap<>(serviziScelti), LocalDateTime.now(), prezzoTotale);

        if (!gestorePersistenza.salva(prenotazione)) {
            return null;
        }

        return prenotazione;
    }

    /*
     * Information Expert: cerca una prenotazione per id (null se inesistente).
     * Esposto perché il Controller risolva le entità tramite il Registro (livello
     * Entity), senza accedere direttamente al livello Database.
     */
    public Prenotazione trovaPrenotazione(long id) {
        return gestorePersistenza.trovaPerId(Prenotazione.class, id);
    }

    /*
     * Information Expert: tutte le prenotazioni di un cliente (lo storico
     * personale), a prescindere dallo stato (Prenotata e Annullata). Usata dal
     * caso d'uso Gestione prenotazioni personali per consultazione e annullamento.
     */
    public List<Prenotazione> prenotazioniCliente(Cliente cliente) {
        return gestorePersistenza.cercaPerCampo(Prenotazione.class, "cliente", cliente);
    }

    /*
     * Annulla una prenotazione (caso d'uso Gestione prenotazioni personali,
     * «include» Annullamento prenotazione): esegue la transizione di stato verso
     * Annullata e rende persistente la modifica. La notifica dell'annullamento
     * (Observer) è a carico del Controller, che la emette dopo aver ricevuto la
     * Prenotazione qui restituita. La verifica del limite temporale è a carico del
     * Controller (Prenotazione.isAnnullabile), come per gli altri controlli di flusso.
     */
    public Prenotazione annullaPrenotazione(Prenotazione prenotazione) {
        // Pattern State: il Context delega allo stato corrente la transizione
        // Prenotata -> Annullata (Annullata sarebbe un no-op).
        prenotazione.annulla();

        return gestorePersistenza.aggiorna(prenotazione);
    }

    /*
     * Prenotazioni attive (stato attivo) per una certa data: si cerca per data e si
     * filtra in Java sullo stato (pattern State, istanze per-prenotazione).
     */
    private List<Prenotazione> prenotazioniAttive(LocalDate data) {
        List<Prenotazione> attive = new ArrayList<>();

        for (Prenotazione p : gestorePersistenza.cercaPerCampo(Prenotazione.class, "data", data)) {
            if (p.getStato() != null && p.getStato().isAttiva()) {
                attive.add(p);
            }
        }

        return attive;
    }
}

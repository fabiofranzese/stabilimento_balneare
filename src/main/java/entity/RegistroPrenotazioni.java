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
 * ombrelloni e del residuo dei servizi, la creazione delle prenotazioni e le
 * query sui conflitti (estensioni 2.a e 3.1.a del flusso Effettua Prenotazione).
 * Non notifica: la notifica al Servizio di Notifica è innescata dal Boundary
 * alla conferma dell'operazione.
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
     * una prenotazione attiva (stato Prenotata) che lo riguarda in quella data.
     * La disponibilità è quindi un dato derivato, non memorizzato sull'ombrellone.
     */
    public boolean isOmbrelloneOccupato(Ombrellone ombrellone, LocalDate data) {
        // Si cerca per ombrellone e data e si filtra in Java sullo stato: gli
        // stati sono istanze per-prenotazione (pattern State), non righe condivise
        // su cui filtrare in query.
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
     * Information Expert: indica se esiste almeno una prenotazione attiva, a
     * prescindere dalla data. È la precondizione della Configurazione
     * stabilimento, che è distruttiva e non va eseguita finché esistono
     * prenotazioni che riferiscono ombrelloni e servizi.
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
     * Disponibilità residua di un servizio in una data: capacità totale meno le
     * quantità incluse nelle prenotazioni attive di quella data. È un dato
     * derivato (la specifica chiede la "disponibilità limitata").
     *
     * Il conteggio si fa in Java perché i criteri generici di GestorePersistenza
     * confrontano campi semplici, non l'appartenenza a una collezione.
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
     * Confronto per id: gli oggetti provengono da caricamenti distinti
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
     * residuo disponibile per la data (null se tutti bastano). Il controllo di
     * conflitto sui servizi (estensione 3.1.a) è una query di dominio: il
     * Controller ne mappa l'esito in un codice, senza eccezioni di controllo.
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
     * di conflitto (isOmbrelloneOccupato, servizioEsaurito) sono verificati a
     * monte dal Controller: qui solo creazione e salvataggio. Restituisce la
     * prenotazione creata, oppure null se il salvataggio fallisce.
     */
    public Prenotazione effettuaPrenotazione(Cliente cliente, Ombrellone ombrellone,
                                             LocalDate data,
                                             Map<ServizioAggiuntivo, Integer> quantitaServizi,
                                             double prezzoTotale) {

        Map<ServizioAggiuntivo, Integer> serviziScelti =
                (quantitaServizi != null) ? quantitaServizi : new LinkedHashMap<>();

        // Pattern State: la prenotazione nasce nello stato Prenotata (istanza
        // nuova, posseduta dalla prenotazione). Il prezzo totale è "congelato"
        // qui, alle tariffe vigenti.
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
     * Il Controller risolve le entità tramite il Registro (livello Entity),
     * senza accedere direttamente al livello Database.
     */
    public Prenotazione trovaPrenotazione(long id) {
        return gestorePersistenza.trovaPerId(Prenotazione.class, id);
    }

    /*
     * Information Expert: tutte le prenotazioni di un cliente (lo storico
     * personale), in qualunque stato. Usata dal caso d'uso Gestione prenotazioni
     * personali per consultazione e annullamento.
     */
    public List<Prenotazione> prenotazioniCliente(Cliente cliente) {
        return gestorePersistenza.cercaPerCampo(Prenotazione.class, "cliente", cliente);
    }

    /*
     * Annulla una prenotazione (caso d'uso Gestione prenotazioni personali):
     * esegue la transizione verso Annullata e rende persistente la modifica.
     * La verifica del limite temporale è a monte, nel Controller
     * (Prenotazione.isAnnullabile), come gli altri controlli di flusso.
     */
    public void annullaPrenotazione(Prenotazione prenotazione) {
        // Pattern State: il Context delega allo stato corrente la transizione
        // Prenotata -> Annullata (Annullata sarebbe un no-op).
        prenotazione.annulla();

        gestorePersistenza.aggiorna(prenotazione);
    }

    /*
     * Prenotazioni attive per una certa data: si cerca per data e si filtra in
     * Java sullo stato (istanze per-prenotazione, pattern State).
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

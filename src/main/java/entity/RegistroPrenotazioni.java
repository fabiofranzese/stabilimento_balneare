package entity;

import database.GestorePersistenza;
import entity.notifica.ServizioNotifica;

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
 * Pattern Observer: è il Subject del ciclo di vita della prenotazione. Dopo aver
 * salvato una prenotazione, notifica gli osservatori registrati (i ServizioNotifica).
 * Gli osservatori sono tenuti in una collezione statica perché i Registro sono
 * creati a ogni chiamata; la registrazione avviene una sola volta nel composition
 * root (setup.Main), così tutte le istanze condividono gli stessi osservatori.
 *
 * Usa GestorePersistenza (livello Database), come gli altri Registro.
 */
public class RegistroPrenotazioni {

    // Osservatori del Subject (Observer pattern), condivisi tra le istanze.
    private static final List<ServizioNotifica> osservatori = new ArrayList<>();

    /*
     * Registra un osservatore del ciclo di vita delle prenotazioni. Invocato dal
     * composition root all'avvio (setup.Main).
     */
    public static void aggiungiOsservatore(ServizioNotifica osservatore) {
        if (osservatore != null && !osservatori.contains(osservatore)) {
            osservatori.add(osservatore);
        }
    }

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
        StatoPrenotazione prenotata = statoPrenotata();

        if (prenotata == null) {
            // Nessuno stato "Prenotata" registrato: non esistono prenotazioni attive.
            return false;
        }

        // Si usa una mappa ordinata per costruire i criteri della ricerca.
        Map<String, Object> criteri = new LinkedHashMap<>();
        criteri.put("ombrellone", ombrellone);
        criteri.put("data", data);
        criteri.put("stato", prenotata);

        return gestorePersistenza.cercaPrimoPerCampi(Prenotazione.class, criteri) != null;
    }

    /*
     * Information Expert: indica se esiste almeno una prenotazione attiva (stato
     * Prenotata), a prescindere dalla data. Usata come precondizione dalla
     * Configurazione stabilimento, che è distruttiva e non deve essere eseguita
     * finché esistono prenotazioni che riferiscono ombrelloni e servizi.
     */
    public boolean esistonoPrenotazioniAttive() {
        StatoPrenotazione prenotata = statoPrenotata();

        if (prenotata == null) {
            return false;
        }

        return gestorePersistenza.cercaPrimoPerCampi(
                Prenotazione.class, Map.of("stato", prenotata)) != null;
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
     * «Creator» + controllo dei conflitti: crea e salva una prenotazione in stato
     * Prenotata, dopo aver verificato disponibilità dell'ombrellone (estensione
     * 2.a) e residuo dei servizi (estensione 3.1.a). Notifica poi gli osservatori
     * (Observer). Lancia eccezioni di dominio se i controlli falliscono.
     */
    public Prenotazione effettuaPrenotazione(Cliente cliente, Ombrellone ombrellone,
                                             LocalDate data,
                                             Map<ServizioAggiuntivo, Integer> quantitaServizi,
                                             double prezzoTotale) {

        if (isOmbrelloneOccupato(ombrellone, data)) {
            throw new OmbrelloneNonDisponibileException(
                    "Ombrellone non disponibile per la data selezionata.");
        }

        Map<ServizioAggiuntivo, Integer> serviziScelti =
                (quantitaServizi != null) ? quantitaServizi : new LinkedHashMap<>();

        // La quantità richiesta di ogni servizio non deve superare il residuo.
        for (Map.Entry<ServizioAggiuntivo, Integer> voce : serviziScelti.entrySet()) {
            int richiesta = (voce.getValue() != null) ? voce.getValue() : 0;
            if (richiesta > 0 && residuoServizio(voce.getKey(), data) < richiesta) {
                throw new ServizioEsauritoException(voce.getKey());
            }
        }

        // Transizione di State: la prenotazione nasce nello stato Prenotata.
        // Il prezzo totale è "congelato" qui, alle tariffe vigenti.
        Prenotazione prenotazione = new Prenotazione(
                data, ombrellone, statoPrenotata(),
                cliente, new LinkedHashMap<>(serviziScelti), LocalDateTime.now(), prezzoTotale);

        if (!gestorePersistenza.salva(prenotazione)) {
            return null;
        }

        // Observer: notifica il Servizio di Notifica (passo 6 del flusso).
        notifica(prenotazione);

        return prenotazione;
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
     * Annullata, rende persistente la modifica e notifica gli osservatori (passo
     * 3.3 del flusso). La verifica del limite temporale è a carico del Controller
     * (Prenotazione.isAnnullabile), come per gli altri controlli di flusso.
     */
    public Prenotazione annullaPrenotazione(Prenotazione prenotazione) {
        // Transizione di State: Prenotata -> Annullata.
        prenotazione.annulla(statoAnnullata());

        Prenotazione aggiornata = gestorePersistenza.aggiorna(prenotazione);

        // Observer: notifica il Servizio di Notifica dell'annullamento.
        notificaAnnullamento(aggiornata);

        return aggiornata;
    }

    /*
     * Notifica tutti gli osservatori registrati che una prenotazione è stata
     * effettuata.
     */
    private void notifica(Prenotazione prenotazione) {
        for (ServizioNotifica osservatore : osservatori) {
            osservatore.prenotazioneEffettuata(prenotazione);
        }
    }

    /*
     * Notifica tutti gli osservatori registrati che una prenotazione è stata
     * annullata.
     */
    private void notificaAnnullamento(Prenotazione prenotazione) {
        for (ServizioNotifica osservatore : osservatori) {
            osservatore.prenotazioneAnnullata(prenotazione);
        }
    }

    /*
     * Prenotazioni attive (stato Prenotata) per una certa data.
     */
    private List<Prenotazione> prenotazioniAttive(LocalDate data) {
        StatoPrenotazione prenotata = statoPrenotata();

        if (prenotata == null) {
            return new ArrayList<>();
        }

        Map<String, Object> criteri = new LinkedHashMap<>();
        criteri.put("data", data);
        criteri.put("stato", prenotata);

        return gestorePersistenza.cercaPerCampi(Prenotazione.class, criteri);
    }

    /*
     * Restituisce lo stato "Prenotata" predisposto all'avvio (riga singleton),
     * oppure null se non presente.
     */
    private StatoPrenotazione statoPrenotata() {
        return gestorePersistenza.cercaPrimoPerCampi(Prenotata.class, Map.of());
    }

    /*
     * Restituisce lo stato "Annullata" predisposto all'avvio (riga singleton),
     * oppure null se non presente.
     */
    private StatoPrenotazione statoAnnullata() {
        return gestorePersistenza.cercaPrimoPerCampi(Annullata.class, Map.of());
    }
}

package entity;

import database.GestorePersistenza;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * RegistroPrenotazioni è il servizio di dominio per le prenotazioni.
 */
public class RegistroPrenotazioni {

    private GestorePersistenza gestorePersistenza;

    public RegistroPrenotazioni() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Un ombrellone è occupato in una certa data se esiste una prenotazione
     * in stato Prenotata che lo riguarda in quella data.
     */
    public boolean isOmbrelloneOccupato(Ombrellone ombrellone, LocalDate data) {
        Map<String, Object> criteri = new LinkedHashMap<>();
        criteri.put("ombrellone", ombrellone);
        criteri.put("data", data);

        for (Prenotazione p : gestorePersistenza.cercaPerCampi(Prenotazione.class, criteri)) {
            if (p.isAttiva()) {
                return true;
            }
        }
        return false;
    }

    /*
     * Indica se esiste almeno una prenotazione attiva, a prescindere dalla data.
     * Questa è la precondizione della Configurazione stabilimento.
     */
    public boolean isPrenotazioniAttivePresenti() {
        for (Prenotazione p : gestorePersistenza.cercaPerCampi(Prenotazione.class, Map.of())) {
            if (p.isAttiva()) {
                return true;
            }
        }
        return false;
    }

    /*
     * Disponibilità residua di un servizio in una data.
     * Calcolata come capacità totale meno le quantità incluse nelle prenotazioni attive di quella data.
     */
    public int residuoServizio(ServizioAggiuntivo servizio, LocalDate data) {
        int usate = 0;

        for (Prenotazione p : prenotazioniAttive(data)) {
            usate += quantitaServizio(p, servizio);
        }

        return servizio.getCapacita() - usate;
    }

    /*
     * Quantità del servizio indicato inclusa in una prenotazione.
     */
    private int quantitaServizio(Prenotazione prenotazione, ServizioAggiuntivo servizio) {
        if (servizio.getId() == null) {
            return 0;
        }
        for (ServizioPrenotato riga : prenotazione.getServiziPrenotati()) {
            if (riga.getServizio() != null && servizio.getId().equals(riga.getServizio().getId())) {
                return riga.getQuantita();
            }
        }
        return 0;
    }

    /*
     * Cerca il primo servizio la cui quantità richiesta supera il
     * residuo disponibile per la data, restituendo null se tutti bastano.
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
     * Crea e salva una prenotazione nello stato Prenotata.
     * Restituisce la prenotazione creata, oppure null se il salvataggio fallisce.
     */
    public Prenotazione effettuaPrenotazione(Cliente cliente, Ombrellone ombrellone,
                                             LocalDate data,
                                             Map<ServizioAggiuntivo, Integer> quantitaServizi,
                                             double prezzoTotale) {

        Map<ServizioAggiuntivo, Integer> serviziScelti =
                (quantitaServizi != null) ? quantitaServizi : new LinkedHashMap<>();

        Prenotazione prenotazione = new Prenotazione(
                data, ombrellone, new Prenotata(),
                cliente, new LinkedHashMap<>(serviziScelti), LocalDateTime.now(), prezzoTotale);

        // La prenotazione referenzia entità staccate: cliente, ombrellone e i servizi
        // (questi ultimi parte della chiave composta di ServizioPrenotato). Si salva
        // con merge, così le associazioni già esistenti vengono ricollegate invece di
        // essere ri-persistite: persist fallirebbe sull'entità staccata propagando
        // lungo la chiave composta. merge restituisce l'istanza gestita.
        return gestorePersistenza.aggiorna(prenotazione);
    }

    /*
     * Cerca una prenotazione per id (null se inesistente).
     */
    public Prenotazione trovaPrenotazione(long id) {
        return gestorePersistenza.trovaPerId(Prenotazione.class, id);
    }

    /*
     * Ottiene tutte le prenotazioni di un cliente (lo storico personale), in qualunque stato.
     */
    public List<Prenotazione> prenotazioniCliente(Cliente cliente) {
        return gestorePersistenza.cercaPerCampo(Prenotazione.class, "cliente", cliente);
    }

    /*
     * Annulla una prenotazione eseguendo la transizione verso Annullata e rendendo persistente la modifica.
     */
    public void annullaPrenotazione(Prenotazione prenotazione) {
        prenotazione.annulla();

        gestorePersistenza.aggiorna(prenotazione);
    }

    /*
     * Prenotazioni attive per una certa data.
     */
    private List<Prenotazione> prenotazioniAttive(LocalDate data) {
        List<Prenotazione> attive = new ArrayList<>();

        for (Prenotazione p : gestorePersistenza.cercaPerCampo(Prenotazione.class, "data", data)) {
            if (p.isAttiva()) {
                attive.add(p);
            }
        }

        return attive;
    }
}

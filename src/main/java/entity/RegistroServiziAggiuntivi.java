package entity;

import database.GestorePersistenza;

import java.util.List;
import java.util.Map;

/*
 * RegistroServiziAggiuntivi è il servizio di dominio per i servizi aggiuntivi.
 */
public class RegistroServiziAggiuntivi {

    private GestorePersistenza gestorePersistenza;

    public RegistroServiziAggiuntivi() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Crea e salva un nuovo servizio aggiuntivo.
     */
    public ServizioAggiuntivo creaServizio(String descrizione, int capacita) {
        ServizioAggiuntivo servizio = new ServizioAggiuntivo(descrizione, capacita);

        if (gestorePersistenza.salva(servizio)) {
            return servizio;
        }

        return null;
    }

    /*
     * Cerca un servizio aggiuntivo per id, ritornando null se inesistente.
     */
    public ServizioAggiuntivo trovaServizio(long id) {
        return gestorePersistenza.trovaPerId(ServizioAggiuntivo.class, id);
    }

    /*
     * Restituisce tutti i servizi aggiuntivi.
     */
    public List<ServizioAggiuntivo> getServizi() {
        return gestorePersistenza.cercaPerCampi(ServizioAggiuntivo.class, Map.of());
    }

    /*
     * Sostituisce l'intero elenco dei servizi con quello indicato nella configurazione dello stabilimento.
     */
    public void sostituisciServizi(String[] descrizioni, int[] capacita) {
        for (ServizioAggiuntivo servizio : getServizi()) {
            gestorePersistenza.elimina(ServizioAggiuntivo.class, servizio.getId());
        }

        for (int i = 0; i < descrizioni.length; i++) {
            creaServizio(descrizioni[i], capacita[i]);
        }
    }
}

package entity;

import database.GestorePersistenza;

import java.util.List;
import java.util.Map;

/*
 * RegistroServiziAggiuntivi è il servizio di dominio per i servizi aggiuntivi
 * (livello Entity, BCED).
 *
 * Ruoli GRASP:
 * - Creator: crea le istanze di ServizioAggiuntivo;
 * - Information Expert: conosce l'insieme dei servizi (getServizi).
 *
 * Usa GestorePersistenza (livello Database), come gli altri Registro.
 */
public class RegistroServiziAggiuntivi {

    private GestorePersistenza gestorePersistenza;

    public RegistroServiziAggiuntivi() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * «Creator»: crea e salva un nuovo servizio aggiuntivo.
     */
    public ServizioAggiuntivo creaServizio(String descrizione, int capacita) {
        ServizioAggiuntivo servizio = new ServizioAggiuntivo(descrizione, capacita);

        if (gestorePersistenza.salva(servizio)) {
            return servizio;
        }

        return null;
    }

    /*
     * Information Expert: restituisce tutti i servizi aggiuntivi.
     */
    public List<ServizioAggiuntivo> getServizi() {
        return gestorePersistenza.cercaPerCampi(ServizioAggiuntivo.class, Map.of());
    }

    /*
     * Caso d'uso Configurazione stabilimento (passo "aggiunge o modifica i
     * servizi"): sostituisce l'intero elenco dei servizi con quello indicato
     * (strategia "replace", coerente con la disposizione). I due array sono
     * paralleli: descrizioni[i] e capacita[i] descrivono il servizio i-esimo.
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

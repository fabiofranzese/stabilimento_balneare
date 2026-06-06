package setup;

import database.GestorePersistenza;
import entity.Annullata;
import entity.Prenotata;
import entity.RegistroUtenti;

import java.util.Map;

/*
 * DatiIniziali predispone i dati di partenza del sistema.
 *
 * Poiché la registrazione è riservata ai Cliente, gli account Gestore vengono
 * creati qui, così da poter dimostrare anche l'accesso come Gestore. Qui vengono
 * anche predisposti gli stati delle prenotazioni.
 *
 * Classe di utilità: non deve essere istanziata.
 */
public final class DatiIniziali {

    private DatiIniziali() {
    }

    /*
     * Crea un account Gestore predefinito, se non già presente.
     * Credenziali: gestore@stabilimento.it / gestore123
     */
    public static void seedGestore() {
        RegistroUtenti registroUtenti = new RegistroUtenti();

        String email = "gestore@stabilimento.it";

        if (registroUtenti.emailEsistente(email)) {
            return;
        }

        registroUtenti.registraGestore(
                "Mario", "Rossi", email, "0810000000", "gestore123");
    }

    /*
     * Predispone gli stati delle prenotazioni come righe "singleton" (una per
     * tipo), se non già presenti. A esse fanno riferimento le prenotazioni create
     * nel caso d'uso Effettua Prenotazione.
     */
    public static void seedStatiPrenotazione() {
        GestorePersistenza gestorePersistenza = new GestorePersistenza();

        if (gestorePersistenza.cercaPrimoPerCampi(Prenotata.class, Map.of()) == null) {
            gestorePersistenza.salva(new Prenotata());
        }

        if (gestorePersistenza.cercaPrimoPerCampi(Annullata.class, Map.of()) == null) {
            gestorePersistenza.salva(new Annullata());
        }
    }
}

package setup;

import entity.RegistroUtenti;

/*
 * DatiIniziali predispone i dati di partenza del sistema.
 *
 * Poiché la registrazione è riservata ai Cliente, gli account Gestore vengono
 * creati qui, così da poter dimostrare anche l'accesso come Gestore.
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
}

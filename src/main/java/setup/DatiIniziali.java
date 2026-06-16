package setup;

import entity.RegistroUtenti;

/*
 * DatiIniziali predispone i dati di partenza del sistema.
 * Poiché la registrazione è riservata ai Cliente, gli account Gestore vengono creati qui.
 * Crea un account Gestore predefinito con credenziali: gestore@stabilimento.it / gestore123
 */
public final class DatiIniziali {

    private DatiIniziali() {
    }

    public static void seedGestore() {
        RegistroUtenti registroUtenti = new RegistroUtenti();

        String email = "gestore@stabilimento.it";

        if (registroUtenti.isEmailEsistente(email)) {
            return;
        }

        registroUtenti.registraGestore(
                "Mario", "Rossi", email, "0810000000", "gestore123");
    }
}

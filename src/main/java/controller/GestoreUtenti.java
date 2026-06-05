package controller;

import entity.Cliente;
import entity.Gestore;
import entity.RegistroUtenti;
import entity.Utente;

/*
 * GestoreUtenti è il Controller (GRASP) dei casi d'uso Registrazione e
 * Accesso al sistema, e funge da Façade tra il Boundary (le finestre Swing)
 * e il dominio (RegistroUtenti).
 *
 * Espone operazioni a grana grossa e restituisce semplici codici interi: il
 * Boundary non conosce le Entity (Cliente, Gestore, Utente) e non importa il
 * package entity, rispettando la separazione BCED.
 */
public class GestoreUtenti {

    // Esiti della registrazione.
    public static final int REGISTRAZIONE_OK = 0;
    public static final int DATI_NON_VALIDI = 1;
    public static final int EMAIL_GIA_REGISTRATA = 2;
    public static final int ERRORE_REGISTRAZIONE = 3;

    // Esiti dell'accesso.
    public static final int LOGIN_CLIENTE = 10;
    public static final int LOGIN_GESTORE = 11;
    public static final int CREDENZIALI_ERRATE = 12;

    // Regola minima per la lunghezza della password (la specifica non la definisce).
    private static final int LUNGHEZZA_MINIMA_PASSWORD = 6;

    /*
     * Caso d'uso Registrazione.
     *
     * 1. valida i dati inseriti (campi non vuoti, email ben formata, password valida);
     * 2. se l'email è già registrata, segnala che si deve procedere con l'accesso;
     * 3. altrimenti crea l'account Cliente.
     */
    public static int registra(String nome, String cognome, String email,
                               String telefono, String password) {

        if (!validaDati(nome, cognome, email, telefono, password)) {
            return DATI_NON_VALIDI;
        }

        RegistroUtenti registroUtenti = new RegistroUtenti();

        // Se l'email esiste già, il flusso prevede di indirizzare l'utente all'accesso.
        if (registroUtenti.emailEsistente(email)) {
            return EMAIL_GIA_REGISTRATA;
        }

        Cliente cliente = registroUtenti.registraCliente(nome, cognome, email, telefono, password);

        if (cliente != null) {
            return REGISTRAZIONE_OK;
        }

        return ERRORE_REGISTRAZIONE;
    }

    /*
     * Caso d'uso Accesso al sistema.
     *
     * Verifica le credenziali e, in base al ruolo associato all'account,
     * restituisce l'esito di login per Cliente o per Gestore.
     */
    public static int accedi(String email, String password) {
        RegistroUtenti registroUtenti = new RegistroUtenti();

        Utente utente = registroUtenti.verificaCredenziali(email, password);

        if (utente == null) {
            return CREDENZIALI_ERRATE;
        }

        if (utente instanceof Cliente) {
            return LOGIN_CLIENTE;
        }

        if (utente instanceof Gestore) {
            return LOGIN_GESTORE;
        }

        // Ramo difensivo: un Utente senza ruolo riconosciuto non dovrebbe esistere.
        return CREDENZIALI_ERRATE;
    }

    /*
     * Validazione dei dati di registrazione (passo "verifica i dati inseriti"
     * del diagramma di sequenza). Tutti i campi devono essere non vuoti, l'email
     * ben formata e la password di lunghezza minima.
     */
    private static boolean validaDati(String nome, String cognome, String email,
                                      String telefono, String password) {

        if (campoVuoto(nome) || campoVuoto(cognome) || campoVuoto(email)
                || campoVuoto(telefono) || campoVuoto(password)) {
            return false;
        }

        if (!emailFormatoValido(email)) {
            return false;
        }

        if (!telefonoValido(telefono)) {
            return false;
        }

        if (!passwordValida(password)) {
            return false;
        }

        return true;
    }

    private static boolean campoVuoto(String valore) {
        return valore == null || valore.trim().isEmpty();
    }

    /*
     * Controllo essenziale del formato dell'email: presenza di una sola @,
     * con testo prima e un dominio con punto dopo.
     */
    private static boolean emailFormatoValido(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    /*
     * Il recapito telefonico deve contenere solo cifre (una o più).
     */
    private static boolean telefonoValido(String telefono) {
        return telefono.matches("\\d+");
    }

    private static boolean passwordValida(String password) {
        return password.length() >= LUNGHEZZA_MINIMA_PASSWORD;
    }
}

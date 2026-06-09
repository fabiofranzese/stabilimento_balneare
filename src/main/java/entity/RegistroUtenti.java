package entity;

import database.GestorePersistenza;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/*
 * RegistroUtenti è il servizio di dominio per gli Utenti (livello Entity, BCED).
 *
 * Ruoli GRASP:
 * - Creator: è responsabile della creazione dei Cliente (e dei Gestore di setup);
 * - Information Expert: conosce l'insieme degli Utenti, quindi gli competono il
 *   controllo di unicità dell'email e la verifica delle credenziali.
 *
 * Usa GestorePersistenza (livello Database) per rendere persistenti gli oggetti:
 * la logica di dominio resta qui, il codice tecnico di persistenza resta nel
 * package database. È la dipendenza Entity -> Database adottata come idioma del
 * progetto per i servizi Registro.
 */
public class RegistroUtenti {

    private GestorePersistenza gestorePersistenza;

    public RegistroUtenti() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Crea e salva un nuovo Cliente (caso d'uso Registrazione).
     * La password ricevuta in chiaro viene cifrata prima della memorizzazione.
     * Restituisce il Cliente salvato, oppure null se il salvataggio fallisce.
     *
     * Il controllo di unicità dell'email è responsabilità del chiamante
     * (vedi emailEsistente): qui ci si limita alla creazione.
     */
    public Cliente registraCliente(String nome, String cognome, String email,
                                   String telefono, String passwordInChiaro) {

        Cliente cliente = new Cliente(nome, cognome, email, telefono, cifra(passwordInChiaro));

        if (gestorePersistenza.salva(cliente)) {
            return cliente;
        }

        return null;
    }

    /*
     * Crea e salva un nuovo Gestore. Non è esposto alla registrazione utente:
     * è usato dal livello setup per predisporre gli account dei gestori.
     *
     * NOTE: nel domain model RegistroUtenti è «Cliente Creator»: la creazione
     * dei Gestore è esclusa dal flusso applicativo. Questo metodo resta solo
     * come helper di bootstrap, invocato dal package setup (DatiIniziali) e non
     * dai casi d'uso; vive qui per riusare la cifratura della password (cifra()).
     */
    public Gestore registraGestore(String nome, String cognome, String email,
                                   String telefono, String passwordInChiaro) {

        Gestore gestore = new Gestore(nome, cognome, email, telefono, cifra(passwordInChiaro));

        if (gestorePersistenza.salva(gestore)) {
            return gestore;
        }

        return null;
    }

    /*
     * Verifica se esiste già un Utente registrato con l'email indicata.
     */
    public boolean emailEsistente(String email) {
        return cercaUtentePerEmail(email) != null;
    }

    /*
     * Cerca l'Utente associato a un'email. Restituisce il sottotipo concreto
     * (Cliente o Gestore) grazie al discriminatore, oppure null se non esiste.
     */
    public Utente cercaUtentePerEmail(String email) {
        return gestorePersistenza.cercaPrimoPerCampi(
                Utente.class,
                Map.of("email", email)
        );
    }

    /*
     * Verifica le credenziali di accesso (caso d'uso Accesso al sistema).
     * Restituisce l'Utente se email e password corrispondono, altrimenti null.
     */
    public Utente verificaCredenziali(String email, String passwordInChiaro) {
        Utente utente = cercaUtentePerEmail(email);

        if (utente == null) {
            return null;
        }

        if (utente.getPassword().equals(cifra(passwordInChiaro))) {
            return utente;
        }

        return null;
    }

    /*
     * Cifra una password con SHA-256 e la restituisce in formato esadecimale.
     * Si usa MessageDigest del JDK, senza dipendenze aggiuntive.
     */
    private static String cifra(String testo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(testo.getBytes(StandardCharsets.UTF_8));

            StringBuilder esadecimale = new StringBuilder();
            for (byte b : hash) {
                esadecimale.append(String.format("%02x", b));
            }

            return esadecimale.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 è sempre disponibile in una JVM standard: non dovrebbe accadere.
            throw new RuntimeException("Algoritmo di cifratura non disponibile", e);
        }
    }
}

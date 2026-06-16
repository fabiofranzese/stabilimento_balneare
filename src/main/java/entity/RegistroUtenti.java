package entity;

import database.GestorePersistenza;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/*
 * RegistroUtenti è il servizio di dominio per gli Utenti (livello Entity, BCED).
 */
public class RegistroUtenti {

    private GestorePersistenza gestorePersistenza;

    public RegistroUtenti() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * REGISTRAZIONE
     *
     * Crea e salva un nuovo Cliente (caso d'uso Registrazione).
     * La password ricevuta in chiaro viene cifrata prima della memorizzazione.
     * Restituisce il Cliente salvato, oppure null se il salvataggio fallisce.
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
     * Crea e salva un nuovo Gestore.
     * Questo è fuori dal flusso applicativo, ma è un helper invocato da DatiIniziali.
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
    public boolean isEmailEsistente(String email) {
        return cercaUtentePerEmail(email) != null;
    }

    /*
     * Cerca l'Utente associato a un'email.
     */
    public Utente cercaUtentePerEmail(String email) {
        return gestorePersistenza.cercaPrimoPerCampi(
                Utente.class,
                Map.of("email", email)
        );
    }

    /*
     * Verifica le credenziali di accesso (caso d'uso Accesso al sistema).
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
     * Cifra la password con SHA-256.
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

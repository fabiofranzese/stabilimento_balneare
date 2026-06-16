package notifica;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * CanaleComunicazioneEsterno è il sistema di notifica esterno COTS.
 *
 * L'invio avviene via Jakarta Mail su un relay SMTP (Brevo): credenziali e
 * parametri sono letti da email.properties.
 */
public class CanaleComunicazioneEsterno {

    private static final String FILE_CONFIG = "/email.properties";

    private final Properties config;

    public CanaleComunicazioneEsterno() {
        this.config = caricaConfig();
    }

    /*
     * Invia un messaggio sul canale esterno (email).
     * L'invio è sincrono dato che l'applicazione è monolitica.
     */
    public void invia(String destinatario, String oggetto, String corpo) {
        if (config == null) {
            System.err.println("Invio notifica email saltato: configurazione "
                    + FILE_CONFIG + " assente o illeggibile.");
            return;
        }

        try {
            Properties proprietaSmtp = new Properties();
            proprietaSmtp.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
            proprietaSmtp.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
            proprietaSmtp.put("mail.smtp.auth", config.getProperty("mail.smtp.auth", "true"));
            proprietaSmtp.put("mail.smtp.starttls.enable",
                    config.getProperty("mail.smtp.starttls.enable", "true"));

            String utente = config.getProperty("email.utente");
            String password = config.getProperty("email.password");
            String mittente = config.getProperty("email.mittente");

            Session sessione = Session.getInstance(proprietaSmtp, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(utente, password);
                }
            });

            MimeMessage messaggio = new MimeMessage(sessione);
            messaggio.setFrom(new InternetAddress(mittente));
            messaggio.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(destinatario));
            messaggio.setSubject(oggetto, "UTF-8");
            messaggio.setText(corpo, "UTF-8");

            Transport.send(messaggio);
        } catch (Exception e) {
            System.err.println("Invio notifica email fallito: " + e.getMessage());
        }
    }

    /*
     * Carica la configurazione SMTP da email.properties..
     */
    private Properties caricaConfig() {
        try (InputStream flusso = getClass().getResourceAsStream(FILE_CONFIG)) {
            if (flusso == null) {
                return null;
            }
            Properties caricate = new Properties();
            caricate.load(flusso);
            return caricate;
        } catch (IOException e) {
            return null;
        }
    }
}

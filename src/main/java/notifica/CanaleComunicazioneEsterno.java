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
 * CanaleComunicazioneEsterno è il sistema di notifica esterno verso cui inviare le
 * notifiche via email. È un componente esterno (COTS), fuori dai livelli BCED (vive
 * nel package `notifica`).
 *
 * Rappresenta l'"adaptee" del pattern Adapter: ha una propria interfaccia
 * (invia(destinatario, oggetto, corpo)). L'Adapter concreto
 * (boundary.notifica.AdapterServizioNotifica), invocato dal Boundary alla conferma
 * dell'operazione, fa da ponte tra il messaggio dell'applicazione e questo canale.
 *
 * L'invio è reale, via Jakarta Mail su un relay SMTP (Brevo): le credenziali e i
 * parametri SMTP sono letti dal classpath (email.properties, fuori da VCS). La
 * notifica è best-effort e successiva alla conferma dell'operazione: ogni errore
 * (configurazione assente, autenticazione, rete) viene loggato su stderr e NON
 * propagato, così non interrompe il flusso applicativo. L'invio avviene su un
 * thread in background per non bloccare l'interfaccia Swing.
 */
public class CanaleComunicazioneEsterno {

    private static final String FILE_CONFIG = "/email.properties";

    // Configurazione SMTP caricata una volta dal classpath; null se assente.
    private final Properties config;

    public CanaleComunicazioneEsterno() {
        this.config = caricaConfig();
    }

    /*
     * Invia un messaggio sul canale esterno (email). Firma volutamente diversa da
     * quella dell'applicazione: è ciò che l'Adapter deve adattare. Fire-and-forget
     * su un thread daemon: il chiamante (Boundary) non si blocca sull'handshake SMTP.
     */
    public void invia(String destinatario, String oggetto, String corpo) {
        Thread invio = new Thread(() -> inviaEmail(destinatario, oggetto, corpo),
                "invio-notifica-email");
        invio.setDaemon(true);
        invio.start();
    }

    /*
     * Invio effettivo dell'email via SMTP (Jakarta Mail). Eseguito sul thread di
     * background. Solo email: in caso di problemi si logga su stderr e si prosegue.
     */
    private void inviaEmail(String destinatario, String oggetto, String corpo) {
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
            // Notifica best-effort: si registra l'errore e si prosegue (l'operazione
            // di dominio è già confermata a monte).
            System.err.println("Invio notifica email fallito: " + e.getMessage());
        }
    }

    /*
     * Carica la configurazione SMTP da email.properties sul classpath; null se il
     * file non c'è (es. credenziali non predisposte) o non è leggibile.
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

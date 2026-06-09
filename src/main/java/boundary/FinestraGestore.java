package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FinestraGestore è la schermata dell'area riservata al Gestore autenticato.
 *
 * Espone i casi d'uso del Gestore: Configurazione stabilimento e Definizione
 * tariffe.
 *
 * La Definizione tariffe è disponibile solo dopo aver configurato lo
 * stabilimento: il relativo pulsante è abilitato solo se esiste almeno una fila.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraGestore.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraGestore {

    private JPanel pannelloGestore;
    private JButton bottoneConfiguraStabilimento;
    private JButton bottoneDefinisciTariffe;
    private JButton bottoneLogout;

    private JFrame frame;

    public FinestraGestore() {
        // Apre il caso d'uso Configurazione stabilimento, nascondendo quest'area:
        // verrà rimostrata al termine (o all'annullamento) della configurazione.
        // La riconfigurazione è distruttiva: se esistono prenotazioni attive non si
        // apre nemmeno la schermata, si mostra subito l'errore.
        bottoneConfiguraStabilimento.addActionListener(e -> apriConfigurazione());

        // Apre il caso d'uso Definizione tariffe, con la stessa logica.
        bottoneDefinisciTariffe.addActionListener(e -> {
            frame.setVisible(false);
            new FormDefinizioneTariffe(frame).apri();
        });

        // Il logout chiude quest'area e riporta alla schermata principale.
        bottoneLogout.addActionListener(e -> tornaAllaSchermataPrincipale());
    }

    public JFrame apri() {
        frame = new JFrame("Area Gestore");
        frame.setContentPane(pannelloGestore);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Anche chiudendo l'area si torna alla schermata principale.
                new FinestraPrincipale().apri();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // A ogni ritorno su quest'area (es. dopo aver configurato lo
                // stabilimento) si riallinea la disponibilità delle tariffe.
                aggiornaDisponibilitaTariffe();
            }
        });
        aggiornaDisponibilitaTariffe();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    /*
     * Apre la Configurazione stabilimento, ma solo se non ci sono prenotazioni
     * attive: la riconfigurazione è distruttiva e ne rimuoverebbe le postazioni.
     * In presenza di prenotazioni attive si mostra subito l'errore, senza aprire
     * la schermata.
     */
    private void apriConfigurazione() {
        if (GestoreStabilimento.prenotazioniAttivePresenti()) {
            JOptionPane.showMessageDialog(frame,
                    "Impossibile riconfigurare lo stabilimento: esistono prenotazioni attive.\n"
                            + "La configurazione può essere modificata solo quando non ci sono "
                            + "prenotazioni attive.",
                    "Prenotazioni presenti", JOptionPane.WARNING_MESSAGE);
            return;
        }

        frame.setVisible(false);
        new FormConfigurazioneStabilimento(frame).apri();
    }

    /*
     * La definizione tariffe ha senso solo a stabilimento configurato: il
     * pulsante è abilitato unicamente se la configurazione è stata effettuata.
     */
    private void aggiornaDisponibilitaTariffe() {
        bottoneDefinisciTariffe.setEnabled(GestoreStabilimento.configurazioneEffettuata());
    }

    private void tornaAllaSchermataPrincipale() {
        frame.dispose();
        new FinestraPrincipale().apri();
    }
}

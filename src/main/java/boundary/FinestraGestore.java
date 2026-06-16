package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FinestraGestore è la schermata dell'area riservata al Gestore autenticato.
 *
 * Espone i casi d'uso del Gestore: Configurazione stabilimento e Definizione
 * tariffe (questa è disponibile solo dopo aver configurato lo stabilimento).
 *
 */
public class FinestraGestore {

    private JPanel pannelloGestore;
    private JButton bottoneConfiguraStabilimento;
    private JButton bottoneDefinisciTariffe;
    private JButton bottoneLogout;

    private JFrame frame;

    public FinestraGestore() {
        // Apre il caso d'uso Configurazione stabilimento.
        bottoneConfiguraStabilimento.addActionListener(e -> {
            frame.setVisible(false);
            new FormConfigurazioneStabilimento(frame).apri();
        });

        // Apre il caso d'uso Definizione tariffe.
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
                new FinestraPrincipale().apri();
            }

            @Override
            public void windowActivated(WindowEvent e) {
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
     * Implementazione precondizione Definizione Tariffe.
     */
    private void aggiornaDisponibilitaTariffe() {
        bottoneDefinisciTariffe.setEnabled(GestoreStabilimento.isConfigurazioneEffettuata());
    }

    private void tornaAllaSchermataPrincipale() {
        frame.dispose();
        new FinestraPrincipale().apri();
    }
}

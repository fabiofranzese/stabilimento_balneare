package boundary;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FinestraGestore è la schermata dell'area riservata al Gestore autenticato.
 *
 * Espone i casi d'uso del Gestore: per ora la Configurazione stabilimento; gli
 * altri (definizione tariffe, monitoraggio, visualizzazione prenotazioni
 * ricevute, ...) verranno aggiunti qui in seguito.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraGestore.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraGestore {

    private JPanel pannelloGestore;
    private JButton bottoneConfiguraStabilimento;
    private JButton bottoneLogout;

    private JFrame frame;

    public FinestraGestore() {
        // Apre il caso d'uso Configurazione stabilimento, nascondendo quest'area:
        // verrà rimostrata al termine (o all'annullamento) della configurazione.
        bottoneConfiguraStabilimento.addActionListener(e -> {
            frame.setVisible(false);
            new FormConfigurazioneStabilimento(frame).apri();
        });

        // Il logout chiude quest'area e riporta alla schermata principale.
        bottoneLogout.addActionListener(e -> tornaAllaSchermataPrincipale());
    }

    public JFrame apri() {
        frame = new JFrame("Area Gestore");
        frame.setContentPane(pannelloGestore);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Anche chiudendo l'area si torna alla schermata principale.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new FinestraPrincipale().apri();
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    private void tornaAllaSchermataPrincipale() {
        frame.dispose();
        new FinestraPrincipale().apri();
    }
}

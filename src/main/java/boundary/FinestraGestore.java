package boundary;

import javax.swing.*;

/*
 * FinestraGestore è la schermata dell'area riservata al Gestore autenticato.
 *
 * Per ora è un segnaposto con il solo pulsante di logout: i casi d'uso del
 * Gestore (configurazione stabilimento, definizione tariffe, monitoraggio,
 * visualizzazione prenotazioni ricevute, ...) verranno aggiunti qui in seguito.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraGestore.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraGestore {

    private JPanel pannelloGestore;
    private JButton bottoneLogout;

    private JFrame frame;

    public FinestraGestore() {
        // Il logout chiude quest'area e riporta alla schermata principale,
        // rimasta aperta sullo sfondo durante la sessione.
        bottoneLogout.addActionListener(e -> frame.dispose());
    }

    public JFrame apri() {
        frame = new JFrame("Area Gestore");
        frame.setContentPane(pannelloGestore);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }
}

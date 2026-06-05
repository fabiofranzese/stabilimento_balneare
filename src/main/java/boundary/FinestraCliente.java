package boundary;

import javax.swing.*;

/*
 * FinestraCliente è la schermata dell'area riservata al Cliente autenticato.
 *
 * Per ora è un segnaposto con il solo pulsante di logout: i casi d'uso del
 * Cliente (visualizzazione mappa, effettua prenotazione, gestione prenotazioni
 * personali, ...) verranno aggiunti qui in seguito.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraCliente.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraCliente {

    private JPanel pannelloCliente;
    private JButton bottoneLogout;

    private JFrame frame;

    public FinestraCliente() {
        // Il logout chiude quest'area e riporta alla schermata principale,
        // rimasta aperta sullo sfondo durante la sessione.
        bottoneLogout.addActionListener(e -> frame.dispose());
    }

    public JFrame apri() {
        frame = new JFrame("Area Cliente");
        frame.setContentPane(pannelloCliente);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }
}

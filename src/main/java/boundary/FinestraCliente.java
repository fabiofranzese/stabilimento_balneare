package boundary;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FinestraCliente è la schermata dell'area riservata al Cliente autenticato.
 *
 * Espone i casi d'uso del Cliente: per ora la Visualizzazione Mappa; gli altri
 * (effettua prenotazione, gestione prenotazioni personali, ...) verranno aggiunti
 * qui in seguito.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraCliente.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraCliente {

    private JPanel pannelloCliente;
    private JButton bottoneVisualizzaMappa;
    private JButton bottoneLogout;

    // Email del cliente autenticato (identità propagata dall'accesso): serve ai
    // casi d'uso che agiscono per conto del cliente, come Effettua Prenotazione.
    private final String emailCliente;

    private JFrame frame;

    public FinestraCliente(String emailCliente) {
        this.emailCliente = emailCliente;

        // Apre il caso d'uso Visualizzazione Mappa, nascondendo quest'area:
        // verrà rimostrata alla chiusura della mappa.
        bottoneVisualizzaMappa.addActionListener(e -> {
            frame.setVisible(false);
            new FormVisualizzazioneMappa(frame, emailCliente).apri();
        });

        // Il logout chiude quest'area e riporta alla schermata principale.
        bottoneLogout.addActionListener(e -> tornaAllaSchermataPrincipale());
    }

    public JFrame apri() {
        frame = new JFrame("Area Cliente");
        frame.setContentPane(pannelloCliente);
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

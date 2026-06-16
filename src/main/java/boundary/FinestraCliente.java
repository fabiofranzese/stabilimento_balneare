package boundary;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FinestraCliente è la schermata dell'area riservata al Cliente autenticato.
 *
 * Espone i casi d'uso del Cliente: la Visualizzazione Mappa (da cui si effettua
 * una prenotazione) e la Gestione delle prenotazioni personali.
 */
public class FinestraCliente {

    private JPanel pannelloCliente;
    private JButton bottoneVisualizzaMappa;
    private JButton bottoneGestionePrenotazioni;
    private JButton bottoneLogout;

    private final String emailCliente;

    private JFrame frame;

    public FinestraCliente(String emailCliente) {
        this.emailCliente = emailCliente;

        // Apre il caso d'uso Visualizzazione Mappa.
        bottoneVisualizzaMappa.addActionListener(e -> {
            frame.setVisible(false);
            new FormVisualizzazioneMappa(frame, emailCliente).apri();
        });

        // Apre il caso d'uso Gestione prenotazioni personali.
        bottoneGestionePrenotazioni.addActionListener(e -> {
            frame.setVisible(false);
            new FormGestionePrenotazioni(frame, emailCliente).apri();
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

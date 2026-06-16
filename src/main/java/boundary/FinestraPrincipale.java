package boundary;

import javax.swing.*;

/*
 * FinestraPrincipale è la schermata iniziale per un UtenteNonAutenticato e
 * offre le due azioni di partenza: registrarsi oppure accedere.
 */
public class FinestraPrincipale {

    private JPanel pannelloPrincipale;
    private JButton bottoneAccedi;
    private JButton bottoneRegistrati;

    private JFrame frame;

    public FinestraPrincipale() {
        bottoneAccedi.addActionListener(e -> {
            frame.setVisible(false);
            new FormAccesso(frame).apri();
        });
        bottoneRegistrati.addActionListener(e -> {
            frame.setVisible(false);
            new FormRegistrazione(frame).apri();
        });
    }

    public JFrame apri() {
        frame = new JFrame("Sistema di Gestione Stabilimenti Balneari");
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
        return frame;
    }
}

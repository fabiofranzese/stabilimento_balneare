package boundary;

import javax.swing.*;

/*
 * FinestraPrincipale è la schermata iniziale per un UtenteNonAutenticato.
 * Offre le due azioni di partenza: registrarsi oppure accedere.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FinestraPrincipale.form):
 * i componenti sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 */
public class FinestraPrincipale {

    private JPanel pannelloPrincipale;
    private JButton bottoneAccedi;
    private JButton bottoneRegistrati;

    private JFrame frame;

    public FinestraPrincipale() {
        // Aprendo una nuova finestra si nasconde quella corrente, così da non
        // avere più finestre aperte contemporaneamente. La finestra chiamante
        // (questa) viene passata al form, che la rimostrerà se viene chiuso.
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
        // La schermata principale chiude l'applicazione quando viene chiusa.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
        return frame;
    }
}

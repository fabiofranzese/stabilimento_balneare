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

    public FinestraPrincipale() {
        bottoneAccedi.addActionListener(e -> new FormAccesso().apri());
        bottoneRegistrati.addActionListener(e -> new FormRegistrazione().apri());
    }

    public JFrame apri() {
        JFrame frame = new JFrame("Sistema di Gestione Stabilimenti Balneari");
        frame.setContentPane(pannelloPrincipale);
        // La schermata principale chiude l'applicazione quando viene chiusa.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return frame;
    }
}

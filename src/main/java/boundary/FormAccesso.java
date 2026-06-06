package boundary;

import controller.GestoreUtenti;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FormAccesso è il Boundary (BCED) del caso d'uso Accesso al sistema.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer (FormAccesso.form):
 * i campi sotto sono bindati al form e istanziati da IntelliJ in compilazione.
 *
 * Raccoglie email e password e li invia al Controller GestoreUtenti, mostrando
 * l'esito del login. Le schermate vere e proprie di Cliente e Gestore saranno
 * realizzate nei casi d'uso successivi: qui ci limitiamo alla conferma di login.
 */
public class FormAccesso {

    private JPanel pannelloAccesso;
    private JTextField campoEmail;
    private JPasswordField campoPassword;
    private JButton bottoneAccedi;

    // Finestra da cui si è aperto l'accesso (la schermata principale), nascosta
    // mentre questo form è aperto: viene rimostrata se il form viene chiuso.
    private final JFrame finestraChiamante;
    // Email da precompilare nel campo (es. quando si arriva dalla registrazione
    // con un'email già registrata); null se non c'è nulla da precompilare.
    private final String emailPrecompilata;
    private JFrame frame;

    public FormAccesso(JFrame finestraChiamante) {
        this(finestraChiamante, null);
    }

    public FormAccesso(JFrame finestraChiamante, String emailPrecompilata) {
        this.finestraChiamante = finestraChiamante;
        this.emailPrecompilata = emailPrecompilata;
        bottoneAccedi.addActionListener(e -> eseguiAccesso());
    }

    public JFrame apri() {
        frame = new JFrame("Accesso");
        frame.setContentPane(pannelloAccesso);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Precompila l'email se fornita dal chiamante.
        if (emailPrecompilata != null) {
            campoEmail.setText(emailPrecompilata);
        }
        // Se l'utente chiude il form senza accedere, si torna alla schermata
        // principale (la finestra chiamante).
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return frame;
    }

    private void eseguiAccesso() {
        String email = campoEmail.getText();
        String password = new String(campoPassword.getPassword());

        int esito = GestoreUtenti.accedi(email, password);

        switch (esito) {
            case GestoreUtenti.LOGIN_CLIENTE:
                // Accesso riuscito: la schermata principale non serve più.
                finestraChiamante.dispose();
                frame.dispose();
                new FinestraCliente().apri();
                break;

            case GestoreUtenti.LOGIN_GESTORE:
                finestraChiamante.dispose();
                frame.dispose();
                new FinestraGestore().apri();
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Email o password non corretti.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }
}

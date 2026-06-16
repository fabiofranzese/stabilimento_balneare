package boundary;

import controller.GestoreUtenti;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FormAccesso è il Boundary del caso d'uso Accesso al sistema.
 * Raccoglie email e password e li invia al Controller GestoreUtenti, mostrando
 * l'esito del login e aprendo l'area riservata corrispondente al ruolo
 * (FinestraCliente o FinestraGestore).
 */
public class FormAccesso {

    private JPanel pannelloAccesso;
    private JTextField campoEmail;
    private JPasswordField campoPassword;
    private JButton bottoneAccedi;

    private final JFrame finestraChiamante;
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
        if (emailPrecompilata != null) {
            campoEmail.setText(emailPrecompilata);
        }
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
                // Si propaga l'email del cliente all'area Cliente.
                finestraChiamante.dispose();
                frame.dispose();
                new FinestraCliente(email).apri();
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

package boundary;

import controller.GestoreUtenti;

import javax.swing.*;

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

    private JFrame frame;

    public FormAccesso() {
        bottoneAccedi.addActionListener(e -> eseguiAccesso());
    }

    public JFrame apri() {
        frame = new JFrame("Accesso");
        frame.setContentPane(pannelloAccesso);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
                JOptionPane.showMessageDialog(frame,
                        "Accesso effettuato come Cliente.",
                        "Accesso", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                break;

            case GestoreUtenti.LOGIN_GESTORE:
                JOptionPane.showMessageDialog(frame,
                        "Accesso effettuato come Gestore.",
                        "Accesso", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Email o password non corretti.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }
}

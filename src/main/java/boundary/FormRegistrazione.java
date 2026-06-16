package boundary;

import controller.GestoreUtenti;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * FormRegistrazione è il Boundary del caso d'uso Registrazione.
 */
public class FormRegistrazione {

    private JPanel pannelloRegistrazione;
    private JTextField campoNome;
    private JTextField campoCognome;
    private JTextField campoEmail;
    private JTextField campoTelefono;
    private JPasswordField campoPassword;
    private JButton bottoneRegistrati;

    private final JFrame finestraChiamante;
    private JFrame frame;

    public FormRegistrazione(JFrame finestraChiamante) {
        this.finestraChiamante = finestraChiamante;
        bottoneRegistrati.addActionListener(e -> eseguiRegistrazione());
    }

    public JFrame apri() {
        frame = new JFrame("Registrazione");
        frame.setContentPane(pannelloRegistrazione);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

    private void eseguiRegistrazione() {
        String nome = campoNome.getText();
        String cognome = campoCognome.getText();
        String email = campoEmail.getText();
        String telefono = campoTelefono.getText();
        String password = new String(campoPassword.getPassword());

        int esito = GestoreUtenti.registra(nome, cognome, email, telefono, password);

        switch (esito) {
            case GestoreUtenti.REGISTRAZIONE_OK:
                finestraChiamante.dispose();
                frame.dispose();
                new FinestraCliente(email).apri();
                break;

            case GestoreUtenti.EMAIL_GIA_REGISTRATA:
                int scelta = JOptionPane.showConfirmDialog(frame,
                        "Esiste già un account con questa email. Vuoi accedere?",
                        "Email già registrata", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (scelta == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    new FormAccesso(finestraChiamante, email).apri();
                }
                break;

            case GestoreUtenti.DATI_NON_VALIDI:
                JOptionPane.showMessageDialog(frame,
                        "Dati non validi: controlla che tutti i campi siano compilati, "
                                + "che l'email sia corretta, che il recapito telefonico contenga solo cifre "
                                + "e che la password abbia almeno 6 caratteri.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Si è verificato un errore durante la registrazione. Riprova.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }
}

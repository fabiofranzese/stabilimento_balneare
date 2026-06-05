package boundary;

import controller.GestoreUtenti;

import javax.swing.*;

/*
 * FormRegistrazione è il Boundary (BCED) del caso d'uso Registrazione.
 *
 * L'interfaccia è realizzata con l'IntelliJ GUI Designer: la disposizione dei
 * componenti è descritta nel file FormRegistrazione.form e i campi sotto sono
 * "bindati" a quel form. È IntelliJ, in fase di compilazione, a istanziare i
 * componenti (metodo generato $$$setupUI$$$) prima del corpo del costruttore.
 *
 * Questa classe contiene solo l'interazione con l'utente: delega ogni logica al
 * Controller GestoreUtenti e non accede né al dominio né alla persistenza.
 */
public class FormRegistrazione {

    private JPanel pannelloRegistrazione;
    private JTextField campoNome;
    private JTextField campoCognome;
    private JTextField campoEmail;
    private JTextField campoTelefono;
    private JPasswordField campoPassword;
    private JButton bottoneRegistrati;

    private JFrame frame;

    public FormRegistrazione() {
        bottoneRegistrati.addActionListener(e -> eseguiRegistrazione());
    }

    /*
     * Costruisce e mostra la finestra di registrazione a partire dal pannello
     * definito nel form.
     */
    public JFrame apri() {
        frame = new JFrame("Registrazione");
        frame.setContentPane(pannelloRegistrazione);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
                // La registrazione effettua direttamente l'accesso: l'utente è
                // ora dentro al sistema come Cliente, senza dover accedere di nuovo.
                JOptionPane.showMessageDialog(frame,
                        "Registrazione completata: accesso effettuato come Cliente.",
                        "Registrazione", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                break;

            case GestoreUtenti.EMAIL_GIA_REGISTRATA:
                // Il flusso prevede di indirizzare l'utente già registrato all'accesso.
                int scelta = JOptionPane.showConfirmDialog(frame,
                        "Esiste già un account con questa email. Vuoi accedere?",
                        "Email già registrata", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (scelta == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    new FormAccesso().apri();
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}

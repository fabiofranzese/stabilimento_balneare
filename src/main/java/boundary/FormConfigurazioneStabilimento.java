package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/*
 * FormConfigurazioneStabilimento è il Boundary (BCED) del caso d'uso
 * Configurazione stabilimento.
 *
 * Interfaccia realizzata con l'IntelliJ GUI Designer
 * (FormConfigurazioneStabilimento.form): i campi sotto sono bindati al form e
 * istanziati da IntelliJ in compilazione (metodo generato $$$setupUI$$$), prima
 * del corpo del costruttore.
 *
 * Il gestore costruisce la disposizione una fila per volta (tipo + numero di
 * ombrelloni) e l'elenco dei servizi aggiuntivi (descrizione + capacità); alla
 * conferma l'intera configurazione viene inviata al Controller. Questa classe
 * contiene solo l'interazione con l'utente: delega ogni logica a
 * GestoreStabilimento e non accede né al dominio né alla persistenza (scambia
 * solo tipi primitivi/array).
 */
public class FormConfigurazioneStabilimento {

    private JPanel pannelloConfigurazione;

    // Sezione "Disposizione file".
    private JComboBox<String> comboTipoFila;
    private JTextField campoNumeroOmbrelloni;
    private JButton bottoneAggiungiFila;
    private JButton bottoneRimuoviFila;
    private JList<String> listaFile;

    // Sezione "Servizi aggiuntivi".
    private JTextField campoDescrizioneServizio;
    private JTextField campoCapacita;
    private JButton bottoneAggiungiServizio;
    private JButton bottoneRimuoviServizio;
    private JList<String> listaServizi;

    private JButton bottoneSalva;

    // Finestra da cui si è aperta la configurazione (l'area Gestore), nascosta
    // mentre questo form è aperto: viene rimostrata al salvataggio o alla chiusura.
    private final JFrame finestraChiamante;
    private JFrame frame;

    // Etichette dei tipi di fila (dal Controller), usate per la combo e l'elenco.
    private final String[] etichetteTipo = GestoreStabilimento.tipiFila();

    // Stato in memoria della configurazione in corso di modifica.
    // File: array paralleli indice-tipo / numero-ombrelloni, in ordine di fila.
    private final List<Integer> tipiFila = new ArrayList<>();
    private final List<Integer> ombrelloniPerFila = new ArrayList<>();
    // Servizi: array paralleli descrizione / capacità.
    private final List<String> descrizioniServizi = new ArrayList<>();
    private final List<Integer> capacitaServizi = new ArrayList<>();

    private final DefaultListModel<String> modelloFile = new DefaultListModel<>();
    private final DefaultListModel<String> modelloServizi = new DefaultListModel<>();

    public FormConfigurazioneStabilimento(JFrame finestraChiamante) {
        this.finestraChiamante = finestraChiamante;
        // Popola la combo dei tipi di fila e collega i modelli alle liste.
        for (String etichetta : etichetteTipo) {
            comboTipoFila.addItem(etichetta);
        }
        listaFile.setModel(modelloFile);
        listaServizi.setModel(modelloServizi);

        bottoneAggiungiFila.addActionListener(e -> aggiungiFila());
        bottoneRimuoviFila.addActionListener(e -> rimuoviFila());
        bottoneAggiungiServizio.addActionListener(e -> aggiungiServizio());
        bottoneRimuoviServizio.addActionListener(e -> rimuoviServizio());
        bottoneSalva.addActionListener(e -> salva());
    }

    public JFrame apri() {
        frame = new JFrame("Configurazione stabilimento");
        frame.setContentPane(pannelloConfigurazione);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Se l'utente chiude il form senza salvare, si torna all'area Gestore.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });
        // Precarica la configurazione già salvata, così il gestore la modifica.
        precaricaConfigurazione();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return frame;
    }

    // --- Disposizione file ---

    private void aggiungiFila() {
        int numero = leggiIntero(campoNumeroOmbrelloni);
        if (numero < 1) {
            JOptionPane.showMessageDialog(frame,
                    "Indica un numero di ombrelloni valido (almeno 1).",
                    "Dati non validi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tipiFila.add(comboTipoFila.getSelectedIndex());
        ombrelloniPerFila.add(numero);
        campoNumeroOmbrelloni.setText("");
        aggiornaListaFile();
    }

    private void rimuoviFila() {
        int selezione = listaFile.getSelectedIndex();
        if (selezione < 0) {
            return;
        }

        tipiFila.remove(selezione);
        ombrelloniPerFila.remove(selezione);
        aggiornaListaFile();
    }

    private void aggiornaListaFile() {
        modelloFile.clear();
        for (int i = 0; i < tipiFila.size(); i++) {
            modelloFile.addElement("Fila " + (i + 1) + " — " + etichetteTipo[tipiFila.get(i)]
                    + " — " + ombrelloniPerFila.get(i) + " ombrelloni");
        }
    }

    // --- Servizi aggiuntivi ---

    private void aggiungiServizio() {
        String descrizione = campoDescrizioneServizio.getText().trim();
        if (descrizione.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Indica una descrizione per il servizio.",
                    "Dati non validi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int capacita = leggiIntero(campoCapacita);
        if (capacita < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Indica una capacità valida (zero o maggiore).",
                    "Dati non validi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        descrizioniServizi.add(descrizione);
        capacitaServizi.add(capacita);
        campoDescrizioneServizio.setText("");
        campoCapacita.setText("");
        aggiornaListaServizi();
    }

    private void rimuoviServizio() {
        int selezione = listaServizi.getSelectedIndex();
        if (selezione < 0) {
            return;
        }

        descrizioniServizi.remove(selezione);
        capacitaServizi.remove(selezione);
        aggiornaListaServizi();
    }

    private void aggiornaListaServizi() {
        modelloServizi.clear();
        for (int i = 0; i < descrizioniServizi.size(); i++) {
            modelloServizi.addElement(descrizioniServizi.get(i)
                    + " — capacità: " + capacitaServizi.get(i));
        }
    }

    // --- Salvataggio ---

    private void salva() {
        int esito = GestoreStabilimento.salvaConfigurazione(
                toIntArray(tipiFila),
                toIntArray(ombrelloniPerFila),
                descrizioniServizi.toArray(new String[0]),
                toIntArray(capacitaServizi));

        switch (esito) {
            case GestoreStabilimento.CONFIGURAZIONE_OK:
                JOptionPane.showMessageDialog(frame,
                        "Configurazione salvata correttamente.",
                        "Configurazione", JOptionPane.INFORMATION_MESSAGE);
                // Configurazione riuscita: si chiude il form e si torna all'area Gestore.
                finestraChiamante.setVisible(true);
                frame.dispose();
                break;

            case GestoreStabilimento.DATI_NON_VALIDI:
                JOptionPane.showMessageDialog(frame,
                        "Dati non validi: definisci almeno una fila (con almeno un ombrellone) "
                                + "e controlla i servizi aggiuntivi.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;

            case GestoreStabilimento.PRENOTAZIONI_PRESENTI:
                JOptionPane.showMessageDialog(frame,
                        "Impossibile riconfigurare lo stabilimento: esistono prenotazioni attive.\n"
                                + "La configurazione può essere modificata solo quando non ci sono "
                                + "prenotazioni attive.",
                        "Prenotazioni presenti", JOptionPane.WARNING_MESSAGE);
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Si è verificato un errore durante il salvataggio. Riprova.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    // --- Precaricamento della configurazione esistente ---

    private void precaricaConfigurazione() {
        int[] tipiCorrenti = GestoreStabilimento.tipiFilaCorrenti();
        int[] ombrelloniCorrenti = GestoreStabilimento.ombrelloniPerFilaCorrenti();
        for (int i = 0; i < tipiCorrenti.length; i++) {
            tipiFila.add(tipiCorrenti[i]);
            ombrelloniPerFila.add(ombrelloniCorrenti[i]);
        }
        aggiornaListaFile();

        String[] descrizioniCorrenti = GestoreStabilimento.descrizioniServiziCorrenti();
        int[] capacitaCorrenti = GestoreStabilimento.capacitaServiziCorrenti();
        for (int i = 0; i < descrizioniCorrenti.length; i++) {
            descrizioniServizi.add(descrizioniCorrenti[i]);
            capacitaServizi.add(capacitaCorrenti[i]);
        }
        aggiornaListaServizi();
    }

    // --- Utilità ---

    /*
     * Legge un intero dal campo; restituisce -1 se il testo non è un numero
     * valido, così i controlli sui minimi (>=1, >=0) intercettano l'errore.
     */
    private int leggiIntero(JTextField campo) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int[] toIntArray(List<Integer> valori) {
        int[] array = new int[valori.size()];
        for (int i = 0; i < valori.size(); i++) {
            array[i] = valori.get(i);
        }
        return array;
    }
}

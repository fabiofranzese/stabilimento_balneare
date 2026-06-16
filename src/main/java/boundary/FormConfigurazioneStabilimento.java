package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * FormConfigurazioneStabilimento è il Boundary del caso d'uso Configurazione stabilimento.
 * Il gestore costruisce la disposizione una fila per volta (tipo + numero di
 * ombrelloni) e l'elenco dei servizi aggiuntivi (descrizione + capacità).
 * Alla conferma, la configurazione viene inviata al Controller.
 */

public class FormConfigurazioneStabilimento {

    private JPanel pannelloConfigurazione;

    // Sezione "Disposizione file".
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

    private final JFrame finestraChiamante;
    private JFrame frame;

    // Stato in memoria della configurazione in corso di modifica.
    // File: numero di ombrelloni per fila, in ordine.
    private final List<Integer> ombrelloniPerFila = new ArrayList<>();
    // Servizi: array paralleli descrizione / capacità.
    private final List<String> descrizioniServizi = new ArrayList<>();
    private final List<Integer> capacitaServizi = new ArrayList<>();

    private final DefaultListModel<String> modelloFile = new DefaultListModel<>();
    private final DefaultListModel<String> modelloServizi = new DefaultListModel<>();

    public FormConfigurazioneStabilimento(JFrame finestraChiamante) {
        this.finestraChiamante = finestraChiamante;
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
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });
        precaricaConfigurazione();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return frame;
    }

    // Disposizione file

    private void aggiungiFila() {
        int numero = leggiIntero(campoNumeroOmbrelloni);
        if (numero < 1) {
            JOptionPane.showMessageDialog(frame,
                    "Indica un numero di ombrelloni valido (almeno 1).",
                    "Dati non validi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ombrelloniPerFila.add(numero);
        campoNumeroOmbrelloni.setText("");
        aggiornaListaFile();
    }

    private void rimuoviFila() {
        int selezione = listaFile.getSelectedIndex();
        if (selezione < 0) {
            return;
        }

        ombrelloniPerFila.remove(selezione);
        aggiornaListaFile();
    }

    /*
     * Ridisegna l'elenco delle file.
     */
    private void aggiornaListaFile() {
        String[] posizioni = GestoreStabilimento.getEtichettePosizioniFile(ombrelloniPerFila.size());
        modelloFile.clear();
        for (int i = 0; i < ombrelloniPerFila.size(); i++) {
            modelloFile.addElement("Fila " + (i + 1) + " — " + posizioni[i]
                    + " — " + ombrelloniPerFila.get(i) + " ombrelloni");
        }
    }

    // Servizi aggiuntivi

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

    // Salvataggio

    private void salva() {
        int esito = GestoreStabilimento.salvaConfigurazione(
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

    // Precaricamento della configurazione esistente

    private void precaricaConfigurazione() {
        // Una riga per fila ("numeroOmbrelloni") e una per servizio
        // ("descrizione", "capacita"); i numeri viaggiano come stringhe.
        for (Map<String, String> fila : GestoreStabilimento.getFileConfigurate()) {
            ombrelloniPerFila.add(Integer.parseInt(fila.get("numeroOmbrelloni")));
        }
        aggiornaListaFile();

        for (Map<String, String> servizio : GestoreStabilimento.getServiziConfigurati()) {
            descrizioniServizi.add(servizio.get("descrizione"));
            capacitaServizi.add(Integer.parseInt(servizio.get("capacita")));
        }
        aggiornaListaServizi();
    }

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

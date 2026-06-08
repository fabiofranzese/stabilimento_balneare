package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/*
 * FormDefinizioneTariffe è il Boundary (BCED) del caso d'uso Definizione tariffe.
 *
 * Interfaccia realizzata con l'IntelliJ GUI Designer
 * (FormDefinizioneTariffe.form): i campi sotto sono bindati al form e istanziati
 * da IntelliJ in compilazione (metodo generato $$$setupUI$$$), prima del corpo
 * del costruttore.
 *
 * Il gestore sceglie un elemento (un tipo di ombrellone o un servizio
 * aggiuntivo), una stagione e un costo, e li aggiunge a un elenco; può anche
 * rimuovere voci dall'elenco. Alla conferma l'elenco viene inviato al Controller,
 * che lo tratta come stato desiderato completo: imposta o aggiorna i prezzi
 * presenti ed elimina le tariffe non più nell'elenco. Sia le modifiche sia le
 * rimozioni si applicano dunque solo al salvataggio.
 * Questa classe contiene solo l'interazione con l'utente: delega ogni logica a
 * GestoreStabilimento e scambia solo tipi primitivi/array.
 */
public class FormDefinizioneTariffe {

    private JPanel pannelloTariffe;
    private JComboBox<String> comboElemento;
    private JComboBox<String> comboStagione;
    private JTextField campoCosto;
    private JButton bottoneImpostaTariffa;
    private JButton bottoneRimuoviTariffa;
    private JList<String> listaTariffe;
    private JButton bottoneSalva;

    // Finestra da cui si è aperta la definizione tariffe (l'area Gestore),
    // nascosta mentre questo form è aperto: viene rimostrata al salvataggio o
    // alla chiusura.
    private final JFrame finestraChiamante;
    private JFrame frame;

    // Etichette dei elementi e delle stagioni (dal Controller), usate per le
    // combo e per l'elenco.
    private final String[] etichetteElemento = GestoreStabilimento.elementiTariffa();
    private final String[] etichetteStagione = GestoreStabilimento.stagioni();

    // Stato in memoria delle tariffe in corso di definizione (array paralleli),
    // con al più una voce per ogni coppia (elemento, stagione).
    private final List<Integer> elementi = new ArrayList<>();
    private final List<Integer> stagioni = new ArrayList<>();
    private final List<Double> costi = new ArrayList<>();

    private final DefaultListModel<String> modelloTariffe = new DefaultListModel<>();

    public FormDefinizioneTariffe(JFrame finestraChiamante) {
        this.finestraChiamante = finestraChiamante;

        for (String etichetta : etichetteElemento) {
            comboElemento.addItem(etichetta);
        }
        for (String etichetta : etichetteStagione) {
            comboStagione.addItem(etichetta);
        }
        listaTariffe.setModel(modelloTariffe);

        bottoneImpostaTariffa.addActionListener(e -> impostaTariffa());
        bottoneRimuoviTariffa.addActionListener(e -> rimuoviTariffa());
        bottoneSalva.addActionListener(e -> salva());
    }

    public JFrame apri() {
        frame = new JFrame("Definizione tariffe");
        frame.setContentPane(pannelloTariffe);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Se l'utente chiude il form senza salvare, si torna all'area Gestore.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });
        // Precarica le tariffe già definite, così il gestore le aggiorna.
        precaricaTariffe();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return frame;
    }

    private void impostaTariffa() {
        int elemento = comboElemento.getSelectedIndex();
        int stagione = comboStagione.getSelectedIndex();

        if (elemento < 0 || stagione < 0) {
            return;
        }

        double costo = leggiCosto(campoCosto);
        if (costo <= 0) {
            JOptionPane.showMessageDialog(frame,
                    "Indica un costo valido (maggiore di zero).",
                    "Dati non validi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Upsert in memoria: se esiste già una tariffa per la stessa coppia
        // (elemento, stagione), se ne aggiorna il costo; altrimenti la si aggiunge.
        int esistente = trovaTariffa(elemento, stagione);
        if (esistente >= 0) {
            costi.set(esistente, costo);
        } else {
            elementi.add(elemento);
            stagioni.add(stagione);
            costi.add(costo);
        }

        campoCosto.setText("");
        aggiornaListaTariffe();
    }

    /*
     * Toglie la tariffa selezionata dall'elenco in memoria. L'eliminazione dal
     * database avviene al salvataggio (riconciliazione lato Controller).
     */
    private void rimuoviTariffa() {
        int selezione = listaTariffe.getSelectedIndex();
        if (selezione < 0) {
            return;
        }

        elementi.remove(selezione);
        stagioni.remove(selezione);
        costi.remove(selezione);
        aggiornaListaTariffe();
    }

    private void aggiornaListaTariffe() {
        modelloTariffe.clear();
        for (int i = 0; i < elementi.size(); i++) {
            modelloTariffe.addElement(etichetteElemento[elementi.get(i)]
                    + " — " + etichetteStagione[stagioni.get(i)]
                    + " — € " + costi.get(i));
        }
    }

    private void salva() {
        int esito = GestoreStabilimento.salvaTariffe(
                toIntArray(elementi),
                toIntArray(stagioni),
                toDoubleArray(costi));

        switch (esito) {
            case GestoreStabilimento.TARIFFE_OK:
                JOptionPane.showMessageDialog(frame,
                        "Tariffe salvate correttamente.",
                        "Definizione tariffe", JOptionPane.INFORMATION_MESSAGE);
                // Salvataggio riuscito: si chiude il form e si torna all'area Gestore.
                finestraChiamante.setVisible(true);
                frame.dispose();
                break;

            case GestoreStabilimento.TARIFFA_NON_VALIDA:
                JOptionPane.showMessageDialog(frame,
                        "Dati non validi: definisci almeno una tariffa, con un costo maggiore di zero.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Si è verificato un errore durante il salvataggio. Riprova.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private void precaricaTariffe() {
        int[] elementiCorrenti = GestoreStabilimento.elementiTariffeCorrenti();
        int[] stagioniCorrenti = GestoreStabilimento.stagioniTariffeCorrenti();
        double[] costiCorrenti = GestoreStabilimento.costiTariffeCorrenti();

        for (int i = 0; i < elementiCorrenti.length; i++) {
            elementi.add(elementiCorrenti[i]);
            stagioni.add(stagioniCorrenti[i]);
            costi.add(costiCorrenti[i]);
        }
        aggiornaListaTariffe();
    }

    // --- Utilità ---

    /*
     * Cerca in memoria la tariffa per una coppia (elemento, stagione);
     * restituisce l'indice nell'elenco oppure -1 se non presente.
     */
    private int trovaTariffa(int elemento, int stagione) {
        for (int i = 0; i < elementi.size(); i++) {
            if (elementi.get(i) == elemento && stagioni.get(i) == stagione) {
                return i;
            }
        }
        return -1;
    }

    /*
     * Legge un costo dal campo; restituisce -1 se il testo non è un numero
     * valido, così il controllo sul minimo (> 0) intercetta l'errore.
     */
    private double leggiCosto(JTextField campo) {
        try {
            return Double.parseDouble(campo.getText().trim().replace(',', '.'));
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

    private double[] toDoubleArray(List<Double> valori) {
        double[] array = new double[valori.size()];
        for (int i = 0; i < valori.size(); i++) {
            array[i] = valori.get(i);
        }
        return array;
    }
}

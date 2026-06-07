package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * FormEffettuaPrenotazione è il Boundary (BCED) del caso d'uso Effettua
 * Prenotazione, estensione («extend») di Visualizzazione Mappa.
 *
 * Mostra il riepilogo dell'ombrellone selezionato (numero, fila, data, prezzo),
 * la lista dei servizi aggiuntivi selezionabili (con prezzo e disponibilità
 * residua) e il totale aggiornato dal vivo. Alla conferma invia la prenotazione
 * al Controller.
 *
 * L'elenco dei servizi è costruito a runtime nel pannelloServizi, perché dipende
 * dai dati (numero e residuo dei servizi per la data). Con il Controller scambia
 * solo tipi semplici (email, id, LocalDate, primitivi): non conosce le Entity,
 * nel rispetto della separazione BCED.
 */
public class FormEffettuaPrenotazione {

    private JPanel pannelloEffettuaPrenotazione;
    private JLabel etichettaRiepilogoOmbrellone;
    private JLabel etichettaRiepilogoFila;
    private JLabel etichettaRiepilogoData;
    private JLabel etichettaPrezzoBase;
    private JPanel pannelloServizi;
    private JLabel etichettaTotale;
    private JButton bottoneConferma;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Finestra da cui si è aperta la prenotazione (la mappa), nascosta mentre
    // questo form è aperto: viene rimostrata alla chiusura.
    private final JFrame finestraChiamante;
    private JFrame frame;

    // Dati della prenotazione in corso.
    private final String emailCliente;
    private final long idOmbrellone;
    private final int numeroOmbrellone;
    private final String etichettaFila;
    private final LocalDate data;

    // Selettori di quantità dei servizi e id corrispondenti (allineati per indice).
    private final List<JSpinner> spinnerServizi = new ArrayList<>();
    private long[] idServiziVisualizzati = new long[0];

    public FormEffettuaPrenotazione(JFrame finestraChiamante, String emailCliente,
                                    long idOmbrellone, int numeroOmbrellone,
                                    String etichettaFila, LocalDate data) {
        this.finestraChiamante = finestraChiamante;
        this.emailCliente = emailCliente;
        this.idOmbrellone = idOmbrellone;
        this.numeroOmbrellone = numeroOmbrellone;
        this.etichettaFila = etichettaFila;
        this.data = data;

        // I servizi vengono impilati verticalmente.
        pannelloServizi.setLayout(new BoxLayout(pannelloServizi, BoxLayout.Y_AXIS));

        bottoneConferma.addActionListener(e -> conferma());
    }

    public JFrame apri() {
        frame = new JFrame("Effettua prenotazione");
        frame.setContentPane(pannelloEffettuaPrenotazione);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Chiudendo senza confermare si torna alla mappa.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });

        // Riepilogo della postazione scelta.
        etichettaRiepilogoOmbrellone.setText("Ombrellone n. " + numeroOmbrellone);
        etichettaRiepilogoFila.setText(etichettaFila != null ? etichettaFila : "");
        etichettaRiepilogoData.setText("Data: " + data.format(FORMATO_DATA));

        double prezzoBase = GestoreStabilimento.prezzoOmbrellone(idOmbrellone, data);
        etichettaPrezzoBase.setText("Prezzo ombrellone: " + formattaPrezzo(prezzoBase));

        costruisciServizi();
        aggiornaTotale();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    /*
     * Costruisce (o ricostruisce) la lista dei servizi aggiuntivi prenotabili per
     * la data: una riga per servizio (descrizione, prezzo unitario, quantità
     * massima) con un selettore di quantità da 0 al residuo. Sono mostrati solo i
     * servizi disponibili (residuo > 0) e con una tariffa definita: il Controller
     * applica già questo filtro.
     */
    private void costruisciServizi() {
        pannelloServizi.removeAll();
        spinnerServizi.clear();

        String[] descrizioni = GestoreStabilimento.descrizioniServizi(data);
        idServiziVisualizzati = GestoreStabilimento.idServizi(data);
        int[] residui = GestoreStabilimento.residuoServizi(data);
        double[] prezzi = GestoreStabilimento.prezziServizi(data);

        if (descrizioni.length == 0) {
            pannelloServizi.add(new JLabel("Nessun servizio aggiuntivo disponibile."));
        } else {
            for (int i = 0; i < descrizioni.length; i++) {
                JPanel riga = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
                riga.setAlignmentX(Component.LEFT_ALIGNMENT);

                riga.add(new JLabel(descrizioni[i]
                        + " — " + formattaPrezzo(prezzi[i])
                        + "  (max: " + residui[i] + ")"));

                // Quantità ordinabile: da 0 (non scelto) fino al residuo disponibile.
                JSpinner selettore = new JSpinner(new SpinnerNumberModel(0, 0, residui[i], 1));
                selettore.addChangeListener(e -> aggiornaTotale());

                spinnerServizi.add(selettore);
                riga.add(selettore);

                pannelloServizi.add(riga);
            }
        }

        pannelloServizi.revalidate();
        pannelloServizi.repaint();
    }

    /*
     * Ricalcola il totale (ombrellone + servizi per le quantità scelte) per la data.
     */
    private void aggiornaTotale() {
        double totale = GestoreStabilimento.prezzoTotale(
                idOmbrellone, idServiziVisualizzati, quantitaCorrenti(), data);
        etichettaTotale.setText("Totale: " + String.format("€ %.2f", totale));
    }

    /*
     * Conferma la prenotazione: la invia al Controller e gestisce l'esito,
     * incluse le estensioni (ombrellone occupato nel frattempo, servizio esaurito).
     */
    private void conferma() {
        int esito = GestoreStabilimento.effettuaPrenotazione(
                emailCliente, idOmbrellone, data, idServiziVisualizzati, quantitaCorrenti());

        switch (esito) {
            case GestoreStabilimento.PRENOTAZIONE_OK:
                JOptionPane.showMessageDialog(frame,
                        "Prenotazione effettuata. Riceverai una notifica di conferma.",
                        "Prenotazione confermata", JOptionPane.INFORMATION_MESSAGE);
                tornaAllaMappa();
                break;

            case GestoreStabilimento.OMBRELLONE_NON_DISPONIBILE:
                // Estensione 2.a: occupato da un altro utente durante la selezione.
                JOptionPane.showMessageDialog(frame,
                        "L'ombrellone è stato appena occupato da un altro utente.\n"
                                + "Scegli un'altra postazione sulla mappa.",
                        "Ombrellone non disponibile", JOptionPane.WARNING_MESSAGE);
                tornaAllaMappa();
                break;

            case GestoreStabilimento.SERVIZIO_ESAURITO:
                // Estensione 3.1.a: un servizio selezionato si è esaurito.
                JOptionPane.showMessageDialog(frame,
                        "La disponibilità di un servizio selezionato è terminata.\n"
                                + "Sono mostrati i servizi ancora disponibili.",
                        "Servizio esaurito", JOptionPane.WARNING_MESSAGE);
                costruisciServizi();
                aggiornaTotale();
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Impossibile completare la prenotazione. Riprova.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private void tornaAllaMappa() {
        finestraChiamante.setVisible(true);
        frame.dispose();
    }

    /*
     * Quantità attualmente impostate nei selettori, allineate a
     * idServiziVisualizzati (0 = servizio non scelto). Le quantità nulle vengono
     * ignorate dal Controller.
     */
    private int[] quantitaCorrenti() {
        int[] quantita = new int[spinnerServizi.size()];

        for (int i = 0; i < spinnerServizi.size(); i++) {
            quantita[i] = ((Number) spinnerServizi.get(i).getValue()).intValue();
        }

        return quantita;
    }

    private static String formattaPrezzo(double prezzo) {
        return prezzo < 0 ? "tariffa n.d." : String.format("€ %.2f", prezzo);
    }
}

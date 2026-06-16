package boundary;

import boundary.notifica.AdapterServizioNotifica;
import controller.GestoreStabilimento;
import notifica.CanaleComunicazioneEsterno;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * FormEffettuaPrenotazione è il Boundary del caso d'uso Effettua Prenotazione.
 *
 * Mostra il riepilogo dell'ombrellone selezionato (numero, fila, data, prezzo),
 * la lista dei servizi aggiuntivi selezionabili (con prezzo e disponibilità
 * residua) e il totale.
 * Alla conferma invia la prenotazione al Controller.
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

    private final JFrame finestraChiamante;
    private JFrame frame;

    // Dati della prenotazione in corso.
    private final String emailCliente;
    private final long idOmbrellone;
    private final int numeroOmbrellone;
    private final String etichettaFila;
    private final LocalDate data;

    private final List<JSpinner> spinnerServizi = new ArrayList<>();
    private long[] idServiziVisualizzati = new long[0];

    // Adapter verso il sistema esterno di notifica.
    private final AdapterServizioNotifica notificatore =
            new AdapterServizioNotifica(new CanaleComunicazioneEsterno());

    public FormEffettuaPrenotazione(JFrame finestraChiamante, String emailCliente,
                                    long idOmbrellone, int numeroOmbrellone,
                                    String etichettaFila, LocalDate data) {
        this.finestraChiamante = finestraChiamante;
        this.emailCliente = emailCliente;
        this.idOmbrellone = idOmbrellone;
        this.numeroOmbrellone = numeroOmbrellone;
        this.etichettaFila = etichettaFila;
        this.data = data;

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

        etichettaRiepilogoOmbrellone.setText("Ombrellone n. " + numeroOmbrellone);
        etichettaRiepilogoFila.setText(etichettaFila != null ? etichettaFila : "");
        etichettaRiepilogoData.setText("Data: " + data.format(FORMATO_DATA));

        double prezzoBase = GestoreStabilimento.getPrezzoTotale(idOmbrellone, null, null, data);
        etichettaPrezzoBase.setText("Prezzo ombrellone: " + formattaPrezzo(prezzoBase));

        costruisciServizi();
        aggiornaTotale();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    /*
     * Costruisce la lista dei servizi aggiuntivi prenotabili per la data:
     * Una riga per servizio (descrizione, prezzo unitario, quantità massima)
     * con un selettore di quantità da 0 al residuo.
     * Sono mostrati solo i servizi disponibili (residuo > 0) e con una tariffa definita.
     */
    private void costruisciServizi() {
        pannelloServizi.removeAll();
        spinnerServizi.clear();

        List<Map<String, String>> servizi = GestoreStabilimento.getServiziPrenotabili(data);
        idServiziVisualizzati = new long[servizi.size()];

        if (servizi.isEmpty()) {
            pannelloServizi.add(new JLabel("Nessun servizio aggiuntivo disponibile."));
        } else {
            for (int i = 0; i < servizi.size(); i++) {
                Map<String, String> servizio = servizi.get(i);
                idServiziVisualizzati[i] = Long.parseLong(servizio.get("id"));
                int residuo = Integer.parseInt(servizio.get("residuo"));
                double prezzo = Double.parseDouble(servizio.get("prezzo"));

                JPanel riga = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
                riga.setAlignmentX(Component.LEFT_ALIGNMENT);

                riga.add(new JLabel(servizio.get("descrizione")
                        + " — " + formattaPrezzo(prezzo)
                        + "  (max: " + residuo + ")"));

                JSpinner selettore = new JSpinner(new SpinnerNumberModel(0, 0, residuo, 1));
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
        double totale = GestoreStabilimento.getPrezzoTotale(
                idOmbrellone, idServiziVisualizzati, quantitaCorrenti(), data);
        etichettaTotale.setText("Totale: " + String.format("€ %.2f", totale));
    }

    /*
     * Conferma la prenotazione, la invia al Controller e gestisce l'esito,
     * eventualmente innescando la notifica.
     */
    private void conferma() {
        int[] quantita = quantitaCorrenti();
        int esito = GestoreStabilimento.effettuaPrenotazione(
                emailCliente, idOmbrellone, data, idServiziVisualizzati, quantita);

        switch (esito) {
            case GestoreStabilimento.PRENOTAZIONE_OK: {
                String corpoNotifica = GestoreStabilimento.getMessaggioNotificaPrenotazione(
                        emailCliente, idOmbrellone, data, idServiziVisualizzati, quantita);
                if (corpoNotifica != null) {
                    notificatore.prenotazioneEffettuata(emailCliente, corpoNotifica);
                }
                JOptionPane.showMessageDialog(frame,
                        "Prenotazione effettuata. Riceverai una notifica di conferma.",
                        "Prenotazione confermata", JOptionPane.INFORMATION_MESSAGE);
                tornaAllaMappa();
                break;
            }

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

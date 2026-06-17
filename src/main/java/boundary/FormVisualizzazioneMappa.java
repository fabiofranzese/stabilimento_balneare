package boundary;

import controller.GestoreStabilimento;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/*
 * FormVisualizzazioneMappa è il Boundary del caso d'uso Visualizzazione Mappa.
 * Il Cliente sceglie una data e vede la mappa degli ombrelloni, con le
 * postazioni evidenziate come disponibili (verde) o occupate (rosso) per quella data.
 */
public class FormVisualizzazioneMappa {

    private JPanel pannelloVisualizzazioneMappa;
    private JSpinner selettoreData;
    private JButton bottoneMostra;
    private JPanel pannelloMappa;
    private JLabel etichettaNumero;
    private JLabel etichettaFila;
    private JLabel etichettaPrezzo;
    private JLabel etichettaDisponibilita;
    private JButton bottonePrenota;

    private static final Color COLORE_LIBERO = new Color(0x81C784);
    private static final Color COLORE_OCCUPATO = new Color(0xE57373);
    private static final int LARGHEZZA_ETICHETTA_FILA = 70;

    private final JFrame finestraChiamante;
    private final String emailCliente;
    private JFrame frame;

    private LocalDate dataCorrente;
    private long idOmbrelloneSelezionato = -1;
    private int numeroSelezionato = -1;
    private String etichettaFilaSelezionata;
    private int indiceFilaSelezionato = -1;
    private boolean ombrelloneSelezionatoDisponibile = false;

    public FormVisualizzazioneMappa(JFrame finestraChiamante, String emailCliente) {
        this.finestraChiamante = finestraChiamante;
        this.emailCliente = emailCliente;

        Date oggi = aMezzanotte(LocalDate.now());
        selettoreData.setModel(new SpinnerDateModel(oggi, oggi, null, Calendar.DAY_OF_MONTH));
        selettoreData.setEditor(new JSpinner.DateEditor(selettoreData, "dd/MM/yyyy"));

        pannelloMappa.setLayout(new BoxLayout(pannelloMappa, BoxLayout.Y_AXIS));

        bottoneMostra.addActionListener(e -> mostraMappa());
        bottonePrenota.addActionListener(e -> prenota());

        resetDettaglio();
    }

    public JFrame apri() {
        frame = new JFrame("Mappa dello stabilimento");
        frame.setContentPane(pannelloVisualizzazioneMappa);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                if (dataCorrente != null) {
                    mostraMappa();
                }
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    /*
     * Costruisce la mappa per la data selezionata.
     */
    private void mostraMappa() {
        dataCorrente = leggiData();

        // Difesa: il limite del selettore vincola le frecce, ma l'utente potrebbe
        // aver digitato a mano una data passata. In tal caso si riporta a oggi.
        if (dataCorrente.isBefore(LocalDate.now())) {
            dataCorrente = LocalDate.now();
            selettoreData.setValue(aMezzanotte(dataCorrente));
            JOptionPane.showMessageDialog(frame,
                    "Non è possibile visualizzare date precedenti a oggi: viene mostrata la data odierna.",
                    "Data non valida", JOptionPane.WARNING_MESSAGE);
        }

        resetDettaglio();

        pannelloMappa.removeAll();

        String[] etichetteFile = GestoreStabilimento.getEtichetteFile();
        // Una lista per fila, con una riga per ombrellone e chiavi "numero", "id" e "disponibile".
        List<List<Map<String, String>>> mappa = GestoreStabilimento.getMappaOmbrelloni(dataCorrente);

        if (etichetteFile.length == 0) {
            pannelloMappa.add(new JLabel("Nessuna postazione configurata."));
        } else {
            // Il mare è sul lato delle prime file: l'indicatore va in cima alla mappa.
            pannelloMappa.add(creaIndicatoreMare());
            for (int i = 0; i < etichetteFile.length; i++) {
                pannelloMappa.add(creaRigaFila(i, etichetteFile[i], mappa.get(i)));
            }
        }

        pannelloMappa.revalidate();
        pannelloMappa.repaint();
    }

    /*
     * Striscia che rappresenta il mare.
     */
    private JComponent creaIndicatoreMare() {
        JLabel mare = new JLabel("～ ～ ～ ～ ～   Mare   ～ ～ ～ ～ ～", SwingConstants.CENTER);
        mare.setOpaque(true);
        mare.setForeground(new Color(0x01579B));
        mare.setBackground(new Color(0xB3E5FC));
        mare.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        mare.setAlignmentX(Component.LEFT_ALIGNMENT);
        mare.setMaximumSize(new Dimension(Integer.MAX_VALUE, mare.getPreferredSize().height));
        return mare;
    }

    /*
     * Crea la riga grafica di una fila, con l'etichetta della fila che resta a sinistra
     * e gli ombrelloni che sono centrati nella riga.
     */
    private JPanel creaRigaFila(int indiceFila, String etichettaFila,
                                List<Map<String, String>> ombrelloni) {
        JPanel riga = new JPanel(new BorderLayout());
        riga.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel etichetta = new JLabel(etichettaFila + ":");
        etichetta.setPreferredSize(new Dimension(LARGHEZZA_ETICHETTA_FILA,
                etichetta.getPreferredSize().height));
        riga.add(etichetta, BorderLayout.WEST);

        JPanel celle = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        for (Map<String, String> ombrellone : ombrelloni) {
            final int numero = Integer.parseInt(ombrellone.get("numero"));
            final long id = Long.parseLong(ombrellone.get("id"));
            final boolean disponibile = Boolean.parseBoolean(ombrellone.get("disponibile"));

            JButton cella = new JButton(String.valueOf(numero));
            cella.setPreferredSize(new Dimension(48, 36));
            cella.setOpaque(true);
            cella.setBackground(disponibile ? COLORE_LIBERO : COLORE_OCCUPATO);
            cella.addActionListener(e -> selezionaOmbrellone(indiceFila, id, numero, etichettaFila, disponibile));

            celle.add(cella);
        }
        riga.add(celle, BorderLayout.CENTER);

        riga.add(Box.createHorizontalStrut(LARGHEZZA_ETICHETTA_FILA), BorderLayout.EAST);

        riga.setMaximumSize(new Dimension(Integer.MAX_VALUE, riga.getPreferredSize().height));

        return riga;
    }

    /*
     * Mostra i dettagli dell'ombrellone selezionato; l'eventuale impossibilità di
     * prenotare viene segnalata al click su "Prenota".
     */
    private void selezionaOmbrellone(int indiceFila, long id, int numero,
                                     String etichettaFilaTesto, boolean disponibile) {
        idOmbrelloneSelezionato = id;
        numeroSelezionato = numero;
        etichettaFilaSelezionata = etichettaFilaTesto;
        indiceFilaSelezionato = indiceFila;
        ombrelloneSelezionatoDisponibile = disponibile;

        etichettaNumero.setText("Ombrellone n. " + numero);
        etichettaFila.setText(etichettaFilaTesto);

        if (!disponibile) {
            // Occupato per la data scelta: non si mostra il prezzo.
            etichettaDisponibilita.setText("Stato: Non disponibile");
            etichettaPrezzo.setText("Non disponibile");
            return;
        }

        double prezzo = GestoreStabilimento.getPrezzoFila(indiceFila, dataCorrente);

        if (prezzo < 0) {
            // Disponibile ma senza tariffa per la stagione: non si può prenotare.
            etichettaDisponibilita.setText("Stato: Disponibile");
            etichettaPrezzo.setText("Tariffa non disponibile");
            return;
        }

        etichettaDisponibilita.setText("Stato: Disponibile");
        etichettaPrezzo.setText("Prezzo (" + GestoreStabilimento.getNomeStagione(dataCorrente)
                + "): " + String.format("€ %.2f", prezzo));
    }

    /*
     * Punto di estensione verso il caso d'uso Effettua Prenotazione.
     * Se la precondizione non è soddisfatta, segnala il motivo con una dialog.
     */
    private void prenota() {
        if (idOmbrelloneSelezionato < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Seleziona un ombrellone dalla mappa.",
                    "Nessuna selezione", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!ombrelloneSelezionatoDisponibile) {
            JOptionPane.showMessageDialog(frame,
                    "Questo ombrellone è già occupato per la data scelta.\n"
                            + "Scegli una postazione disponibile sulla mappa.",
                    "Ombrellone non disponibile", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (GestoreStabilimento.getPrezzoFila(indiceFilaSelezionato, dataCorrente) < 0) {
            JOptionPane.showMessageDialog(frame,
                    "La tariffa per questa fila non è ancora stata definita.\n"
                            + "Non è possibile prenotare finché il gestore non la imposta.",
                    "Tariffa non disponibile", JOptionPane.WARNING_MESSAGE);
            return;
        }

        frame.setVisible(false);
        new FormEffettuaPrenotazione(frame, emailCliente, idOmbrelloneSelezionato,
                numeroSelezionato, etichettaFilaSelezionata, dataCorrente).apri();
    }


    private void resetDettaglio() {
        idOmbrelloneSelezionato = -1;
        numeroSelezionato = -1;
        etichettaFilaSelezionata = null;
        indiceFilaSelezionato = -1;
        ombrelloneSelezionatoDisponibile = false;
        etichettaNumero.setText("Ombrellone n. -");
        etichettaFila.setText("Fila -");
        etichettaPrezzo.setText("Prezzo -");
        etichettaDisponibilita.setText("Stato: -");
    }

    private LocalDate leggiData() {
        Date data = (Date) selettoreData.getValue();
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }


    private static Date aMezzanotte(LocalDate data) {
        return Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

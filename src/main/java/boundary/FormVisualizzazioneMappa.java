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
 * FormVisualizzazioneMappa è il Boundary (BCED) del caso d'uso Visualizzazione
 * Mappa: il Cliente sceglie una data e vede la mappa degli ombrelloni, con le
 * postazioni evidenziate come disponibili (verde) o occupate (rosso) per quella
 * data.
 *
 * La struttura del form (selettore data, pulsante, area mappa, pannello di
 * dettaglio) è descritta nel file .form (IntelliJ GUI Designer); la mappa vera e
 * propria è invece costruita a runtime nel pannelloMappa, perché dipende dai dati
 * (numero di file e ombrelloni). Cliccando un ombrellone se ne vedono i dettagli
 * (numero, fila, prezzo, disponibilità) e si abilita "Prenota" se è disponibile.
 *
 * Scambia con il Controller solo tipi del JDK (primitivi/array, LocalDate e
 * righe di stringhe a chiavi): non conosce le Entity, nel rispetto della
 * separazione BCED.
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

    // Colori delle celle della mappa.
    private static final Color COLORE_LIBERO = new Color(0x81C784);
    private static final Color COLORE_OCCUPATO = new Color(0xE57373);

    // Larghezza fissa dell'etichetta di fila: la tiene a sinistra e, bilanciata da
    // uno spazio uguale a destra, lascia gli ombrelloni centrati nella riga.
    private static final int LARGHEZZA_ETICHETTA_FILA = 70;

    // Finestra da cui si è aperta la mappa (l'area Cliente), nascosta mentre
    // questo form è aperto: viene rimostrata alla chiusura.
    private final JFrame finestraChiamante;
    // Email del cliente autenticato, propagata al caso d'uso Effettua Prenotazione.
    private final String emailCliente;
    private JFrame frame;

    // Data per cui è mostrata la mappa e ombrellone attualmente selezionato.
    private LocalDate dataCorrente;
    private long idOmbrelloneSelezionato = -1;
    private int numeroSelezionato = -1;
    private String etichettaFilaSelezionata;
    private boolean ombrellonePrenotabile = false;

    public FormVisualizzazioneMappa(JFrame finestraChiamante, String emailCliente) {
        this.finestraChiamante = finestraChiamante;
        this.emailCliente = emailCliente;

        // Selettore di data (gg/mm/aaaa) che parte da oggi e non consente di
        // selezionare date passate: consultare o prenotare nel passato non ha senso.
        Date oggi = aMezzanotte(LocalDate.now());
        selettoreData.setModel(new SpinnerDateModel(oggi, oggi, null, Calendar.DAY_OF_MONTH));
        selettoreData.setEditor(new JSpinner.DateEditor(selettoreData, "dd/MM/yyyy"));

        // La mappa viene impilata per file.
        pannelloMappa.setLayout(new BoxLayout(pannelloMappa, BoxLayout.Y_AXIS));

        bottoneMostra.addActionListener(e -> mostraMappa());
        bottonePrenota.addActionListener(e -> prenota());

        resetDettaglio();
    }

    public JFrame apri() {
        frame = new JFrame("Mappa dello stabilimento");
        frame.setContentPane(pannelloVisualizzazioneMappa);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Chiudendo il form si torna all'area Cliente.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // Tornando sulla mappa (es. dopo una prenotazione), si ricalcola la
                // disponibilità per la data corrente, così la cella appena prenotata
                // appare occupata. Al primo avvio non c'è ancora una mappa da aggiornare.
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
        // Una lista per fila, con una riga per ombrellone: chiavi "numero", "id"
        // e "disponibile" ("true" = libero).
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
     * Striscia che rappresenta il mare, mostrata in cima alla mappa (lato delle
     * prime file).
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
     * Crea la riga grafica di una fila: l'etichetta della fila resta a sinistra
     * (larghezza fissa) e gli ombrelloni sono centrati nella riga. Uno spazio a
     * destra, largo quanto l'etichetta, bilancia quest'ultima così le celle
     * risultano centrate rispetto all'intera mappa.
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

        // Riga larga quanto la mappa: BoxLayout la estende e le celle si centrano.
        riga.setMaximumSize(new Dimension(Integer.MAX_VALUE, riga.getPreferredSize().height));

        return riga;
    }

    /*
     * Mostra i dettagli dell'ombrellone selezionato e abilita "Prenota" se è
     * disponibile per la data scelta.
     */
    private void selezionaOmbrellone(int indiceFila, long id, int numero,
                                     String etichettaFilaTesto, boolean disponibile) {
        idOmbrelloneSelezionato = id;
        numeroSelezionato = numero;
        etichettaFilaSelezionata = etichettaFilaTesto;
        ombrellonePrenotabile = false;

        etichettaNumero.setText("Ombrellone n. " + numero);
        etichettaFila.setText(etichettaFilaTesto);

        if (!disponibile) {
            // Occupato per la data scelta: non si mostra il prezzo.
            etichettaDisponibilita.setText("Stato: Non disponibile");
            etichettaPrezzo.setText("Non disponibile");
            bottonePrenota.setEnabled(false);
            return;
        }

        double prezzo = GestoreStabilimento.getPrezzoFila(indiceFila, dataCorrente);

        if (prezzo < 0) {
            // Disponibile ma senza tariffa per la stagione: non si può prenotare.
            etichettaDisponibilita.setText("Stato: Disponibile");
            etichettaPrezzo.setText("Tariffa non disponibile");
            bottonePrenota.setEnabled(false);
            return;
        }

        etichettaDisponibilita.setText("Stato: Disponibile");
        etichettaPrezzo.setText("Prezzo (" + GestoreStabilimento.getNomeStagione(dataCorrente)
                + "): " + String.format("€ %.2f", prezzo));
        ombrellonePrenotabile = true;
        bottonePrenota.setEnabled(true);
    }

    /*
     * Punto di estensione «extend» verso il caso d'uso Effettua Prenotazione:
     * apre il form di prenotazione per l'ombrellone selezionato e la data corrente,
     * nascondendo la mappa. Alla chiusura la mappa torna visibile e si aggiorna
     * (windowActivated), così l'eventuale nuova prenotazione si riflette subito.
     */
    private void prenota() {
        if (!ombrellonePrenotabile || idOmbrelloneSelezionato < 0) {
            return;
        }

        frame.setVisible(false);
        new FormEffettuaPrenotazione(frame, emailCliente, idOmbrelloneSelezionato,
                numeroSelezionato, etichettaFilaSelezionata, dataCorrente).apri();
    }

    // --- Utilità ---

    private void resetDettaglio() {
        idOmbrelloneSelezionato = -1;
        numeroSelezionato = -1;
        etichettaFilaSelezionata = null;
        ombrellonePrenotabile = false;
        etichettaNumero.setText("Ombrellone n. -");
        etichettaFila.setText("Fila -");
        etichettaPrezzo.setText("Prezzo -");
        etichettaDisponibilita.setText("Stato: -");
        bottonePrenota.setEnabled(false);
    }

    private LocalDate leggiData() {
        Date data = (Date) selettoreData.getValue();
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /*
     * Converte una data nel Date alla mezzanotte locale, formato atteso dal
     * SpinnerDateModel.
     */
    private static Date aMezzanotte(LocalDate data) {
        return Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

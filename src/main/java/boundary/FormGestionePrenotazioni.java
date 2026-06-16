package boundary;

import boundary.notifica.AdapterServizioNotifica;
import controller.GestoreStabilimento;
import notifica.CanaleComunicazioneEsterno;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

/*
 * FormGestionePrenotazioni è il Boundary del caso d'uso Gestione prenotazioni personali.
 */
public class FormGestionePrenotazioni {

    private JPanel pannelloGestionePrenotazioni;
    private JList<String> listaPrenotazioni;
    private JLabel etichettaData;
    private JLabel etichettaPostazione;
    private JLabel etichettaServizi;
    private JLabel etichettaStato;
    private JLabel etichettaPrezzo;
    private JLabel etichettaAvviso;
    private JButton bottoneAnnulla;
    private JButton bottoneChiudi;

    private final JFrame finestraChiamante;
    private JFrame frame;

    private final String emailCliente;

    private final DefaultListModel<String> modelloPrenotazioni = new DefaultListModel<>();

    private List<Map<String, String>> prenotazioni = List.of();

    // Adapter verso il sistema esterno di notifica.
    private final AdapterServizioNotifica notificatore =
            new AdapterServizioNotifica(new CanaleComunicazioneEsterno());

    public FormGestionePrenotazioni(JFrame finestraChiamante, String emailCliente) {
        this.finestraChiamante = finestraChiamante;
        this.emailCliente = emailCliente;

        listaPrenotazioni.setModel(modelloPrenotazioni);
        listaPrenotazioni.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaPrenotazioni.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostraDettaglio();
            }
        });

        etichettaAvviso.setForeground(new Color(0xC62828));

        bottoneAnnulla.addActionListener(e -> annulla());
        bottoneChiudi.addActionListener(e -> chiudi());

        resetDettaglio();
    }

    public JFrame apri() {
        frame = new JFrame("Gestione prenotazioni");
        frame.setContentPane(pannelloGestionePrenotazioni);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finestraChiamante.setVisible(true);
            }
        });

        caricaPrenotazioni();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    /*
     * Carica l'elenco delle prenotazioni del cliente dal Controller.
     */
    private void caricaPrenotazioni() {
        prenotazioni = GestoreStabilimento.getPrenotazioniCliente(emailCliente);

        // Ogni voce della lista riporta la data e lo stato della prenotazione
        // (es. "07/06/2026 - Prenotata").
        modelloPrenotazioni.clear();
        for (Map<String, String> prenotazione : prenotazioni) {
            modelloPrenotazioni.addElement(prenotazione.get("data") + " - " + prenotazione.get("stato"));
        }

        if (prenotazioni.isEmpty()) {
            // Estensione 2.a: nessuna prenotazione.
            etichettaData.setText("Non hai prenotazioni.");
        }

        listaPrenotazioni.clearSelection();
        resetDettaglio();
    }

    /*
     * Mostra il dettaglio della prenotazione selezionata e abilita "Annulla" solo
     * se la prenotazione è ancora annullabile.
     */
    private void mostraDettaglio() {
        int selezione = listaPrenotazioni.getSelectedIndex();

        if (selezione < 0 || selezione >= prenotazioni.size()) {
            resetDettaglio();
            return;
        }

        Map<String, String> riga = prenotazioni.get(selezione);
        boolean annullabile = Boolean.parseBoolean(riga.get("annullabile"));
        String postazione = riga.get("postazione");

        etichettaData.setText("Data: " + riga.get("data"));
        etichettaPostazione.setText("Postazione: " + (postazione.isEmpty() ? "-" : postazione));
        etichettaServizi.setText("Servizi: " + riga.get("servizi"));
        etichettaStato.setText("Stato: " + riga.get("stato"));
        etichettaPrezzo.setText(String.format("Totale: € %.2f", Double.parseDouble(riga.get("prezzo"))));

        bottoneAnnulla.setEnabled(annullabile);

        if (!annullabile && "Prenotata".equals(riga.get("stato"))) {
            etichettaAvviso.setText("Non più annullabile: è oltre il limite temporale.");
        } else {
            etichettaAvviso.setText(" ");
        }
    }

    /*
     * Annulla la prenotazione selezionata, verificando l'annullabilità,
     * inviando la richiesta al Controller, gestendo l'esito
     * ed eventualmente innescando la notifica.
     */
    private void annulla() {
        int selezione = listaPrenotazioni.getSelectedIndex();

        if (selezione < 0 || selezione >= prenotazioni.size()) {
            JOptionPane.showMessageDialog(frame,
                    "Seleziona una prenotazione da annullare.",
                    "Nessuna selezione", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!Boolean.parseBoolean(prenotazioni.get(selezione).get("annullabile"))) {
            JOptionPane.showMessageDialog(frame,
                    "Questa prenotazione non può essere annullata:\n"
                            + "è già annullata oppure è oltre il limite temporale consentito\n"
                            + "(l'annullamento è possibile solo prima della data prenotata).",
                    "Annullamento non consentito", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int conferma = JOptionPane.showConfirmDialog(frame,
                "Vuoi annullare la prenotazione selezionata?",
                "Conferma annullamento", JOptionPane.YES_NO_OPTION);
        if (conferma != JOptionPane.YES_OPTION) {
            return;
        }

        long idAnnullata = Long.parseLong(prenotazioni.get(selezione).get("id"));
        int esito = GestoreStabilimento.annullaPrenotazione(emailCliente, idAnnullata);

        switch (esito) {
            case GestoreStabilimento.ANNULLAMENTO_OK: {
                // Ricevuta la conferma, il Boundary innesca chiedendo al Controller
                // il testo del messaggio e inviandolo tramite l'Adapter.
                String corpoNotifica = GestoreStabilimento.getMessaggioNotificaAnnullamento(emailCliente, idAnnullata);
                if (corpoNotifica != null) {
                    notificatore.prenotazioneAnnullata(emailCliente, corpoNotifica);
                }
                JOptionPane.showMessageDialog(frame,
                        "Prenotazione annullata. Riceverai una notifica di conferma.",
                        "Annullamento effettuato", JOptionPane.INFORMATION_MESSAGE);
                caricaPrenotazioni();
                break;
            }

            case GestoreStabilimento.LIMITE_TEMPORALE_SUPERATO:
                // Estensione 3.2.a: oltre il limite temporale.
                JOptionPane.showMessageDialog(frame,
                        "Operazione negata: la prenotazione è oltre il limite temporale consentito\n"
                                + "(l'annullamento è possibile solo prima della data prenotata).",
                        "Annullamento non consentito", JOptionPane.WARNING_MESSAGE);
                caricaPrenotazioni();
                break;

            case GestoreStabilimento.PRENOTAZIONE_NON_TROVATA:
                JOptionPane.showMessageDialog(frame,
                        "La prenotazione selezionata non è più disponibile.",
                        "Prenotazione non trovata", JOptionPane.WARNING_MESSAGE);
                caricaPrenotazioni();
                break;

            default:
                JOptionPane.showMessageDialog(frame,
                        "Si è verificato un errore durante l'annullamento. Riprova.",
                        "Errore", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private void resetDettaglio() {
        if (!prenotazioni.isEmpty()) {
            etichettaData.setText("Seleziona una prenotazione");
        }
        etichettaPostazione.setText("Postazione: -");
        etichettaServizi.setText("Servizi: -");
        etichettaStato.setText("Stato: -");
        etichettaPrezzo.setText("Totale: -");
        etichettaAvviso.setText(" ");
        bottoneAnnulla.setEnabled(false);
    }

    private void chiudi() {
        finestraChiamante.setVisible(true);
        frame.dispose();
    }
}

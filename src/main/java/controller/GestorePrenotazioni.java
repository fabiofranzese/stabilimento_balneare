package controller;

import entity.Cliente;
import entity.Ombrellone;
import entity.Prenotazione;
import entity.RegistroPrenotazioni;
import entity.ServizioAggiuntivo;
import entity.StatoPrenotazione;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * GestorePrenotazioni è il Controller e la Façade dei casi d'uso Effettua
 * Prenotazione e Gestione prenotazioni personali. Si appoggia a GestoreStabilimento
 * per la disponibilità e i prezzi dello stabilimento e a GestoreUtenti per il cliente.
 */
public class GestorePrenotazioni {

    // Formato di data per le righe dell'elenco prenotazioni (Gestione personali).
    private static final DateTimeFormatter FORMATO_DATA_PRENOTAZIONE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Esiti della prenotazione.
    public static final int PRENOTAZIONE_OK = 20;
    public static final int OMBRELLONE_NON_DISPONIBILE = 21;
    public static final int SERVIZIO_ESAURITO = 22;
    public static final int DATI_PRENOTAZIONE_NON_VALIDI = 23;
    public static final int ERRORE_PRENOTAZIONE = 24;

    // Esiti dell'annullamento (caso d'uso Gestione prenotazioni personali).
    public static final int ANNULLAMENTO_OK = 30;
    public static final int LIMITE_TEMPORALE_SUPERATO = 31;
    public static final int PRENOTAZIONE_NON_TROVATA = 32;
    public static final int ERRORE_ANNULLAMENTO = 33;

    /* EFFETTUA PRENOTAZIONE
     *
     * Operazioni del caso d'uso Effettua Prenotazione (estensione di
     * Visualizzazione Mappa). Il Boundary identifica cliente, ombrellone e
     * servizi con valori semplici (email, id)
     */

    /*
     * Trova il cliente e l'ombrellone e costruisce la mappa
     * servizio→quantità. Verifica i conflitti interrogando il RegistroPrenotazioni
     * per la disponibilità dell'ombrellone e il residuo dei servizi. Restituisce
     * l'esito in un codice per il Boundary e delega la creazione al Registro.
     */
    public static int effettuaPrenotazione(String emailCliente, long idOmbrellone,
                                           LocalDate data, long[] idServiziScelti, int[] quantita) {

        if (emailCliente == null || data == null) {
            return DATI_PRENOTAZIONE_NON_VALIDI;
        }

        if ((idServiziScelti == null) != (quantita == null)
                || (idServiziScelti != null && idServiziScelti.length != quantita.length)) {
            return DATI_PRENOTAZIONE_NON_VALIDI;
        }

        Cliente cliente = GestoreUtenti.trovaCliente(emailCliente);
        Ombrellone ombrellone = GestoreStabilimento.trovaOmbrellone(idOmbrellone);

        if (cliente == null || ombrellone == null) {
            return DATI_PRENOTAZIONE_NON_VALIDI;
        }

        Map<ServizioAggiuntivo, Integer> quantitaServizi = new LinkedHashMap<>();
        if (idServiziScelti != null) {
            for (int i = 0; i < idServiziScelti.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio = GestoreStabilimento.trovaServizio(idServiziScelti[i]);
                if (servizio == null) {
                    return DATI_PRENOTAZIONE_NON_VALIDI;
                }
                quantitaServizi.put(servizio, quantita[i]);
            }
        }

        double totale = getPrezzoTotale(idOmbrellone, idServiziScelti, quantita, data);

        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();

        if (registroPrenotazioni.isOmbrelloneOccupato(ombrellone, data)) {
            // Estensione 2.a: ombrellone già occupato per la data scelta.
            return OMBRELLONE_NON_DISPONIBILE;
        }
        if (registroPrenotazioni.servizioEsaurito(quantitaServizi, data) != null) {
            // Estensione 3.1.a: residuo di un servizio selezionato insufficiente.
            return SERVIZIO_ESAURITO;
        }

        try {
            Prenotazione prenotazione = registroPrenotazioni
                    .effettuaPrenotazione(cliente, ombrellone, data, quantitaServizi, totale);
            if (prenotazione == null) {
                return ERRORE_PRENOTAZIONE;
            }

            return PRENOTAZIONE_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_PRENOTAZIONE;
        }
    }

    /*
     * Servizi prenotabili per la data: delega allo stabilimento, che possiede
     * tariffe e disponibilità residua.
     */
    public static List<Map<String, String>> getServiziPrenotabili(LocalDate data) {
        return GestoreStabilimento.getServiziPrenotabili(data);
    }

    /*
     * Prezzo totale della prenotazione (ombrellone + servizi): delega allo stabilimento.
     */
    public static double getPrezzoTotale(long idOmbrellone, long[] idServiziScelti,
                                         int[] quantita, LocalDate data) {
        return GestoreStabilimento.getPrezzoTotale(idOmbrellone, idServiziScelti, quantita, data);
    }

    /* GESTIONE PRENOTAZIONI PERSONALI
     *
     * Operazioni del caso d'uso Gestione prenotazioni personali (attore
     * ClienteAutenticato): consultazione dello storico e annullamento entro il
     * limite temporale. Il Boundary identifica il cliente con l'email e la
    /* prenotazione con il suo id.

    /*
     * Annullamento prenotazione.
     * Ottiene cliente per email e prenotazione per id e verifica che la
     * prenotazione appartenga a quel cliente e che sia annullabile entro il
     * limite temporale. Delega al RegistroPrenotazioni la transizione ad
     * Annullata e il salvataggio.
     */
    public static int annullaPrenotazione(String emailCliente, long idPrenotazione) {
        if (emailCliente == null) {
            return PRENOTAZIONE_NON_TROVATA;
        }

        Cliente cliente = GestoreUtenti.trovaCliente(emailCliente);
        if (cliente == null) {
            return PRENOTAZIONE_NON_TROVATA;
        }

        Prenotazione prenotazione =
                new RegistroPrenotazioni().trovaPrenotazione(idPrenotazione);

        // La prenotazione deve esistere ed essere del cliente richiedente.
        if (prenotazione == null || !appartieneA(prenotazione, cliente)) {
            return PRENOTAZIONE_NON_TROVATA;
        }

        // Estensione 3.2.a: oltre il limite temporale (o già annullata).
        if (!prenotazione.isAnnullabile(LocalDate.now())) {
            return LIMITE_TEMPORALE_SUPERATO;
        }

        try {
            new RegistroPrenotazioni().annullaPrenotazione(prenotazione);

            return ANNULLAMENTO_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_ANNULLAMENTO;
        }
    }

    /*
     * Storico del cliente: una riga per prenotazione, in
     * ordine deterministico (per data, poi per id), con chiavi
     *   "data"`: data prenotata;
     *   "postazione": postazione scelta ("Ombrellone n. X (Fila Y)");
     *   "servizi": servizi aggiuntivi con quantità, o "nessuno";
     *   "stato": nome dello stato (Prenotata/Annullata);
     *   "prezzo": prezzo totale;
     *   "id": id della prenotazione;
     *   "annullabile": true se l'annullamento è possibile (stato Prenotata e oggi < data), altrimenti false
     */
    public static List<Map<String, String>> getPrenotazioniCliente(String emailCliente) {
        Cliente cliente = GestoreUtenti.trovaCliente(emailCliente);
        if (cliente == null) {
            return List.of();
        }

        List<Prenotazione> prenotazioni = new ArrayList<>(cliente.getPrenotazioni());
        prenotazioni.sort(Comparator.comparing(Prenotazione::getData)
                .thenComparing(Prenotazione::getId));

        LocalDate oggi = LocalDate.now();
        List<Map<String, String>> righe = new ArrayList<>();

        for (Prenotazione prenotazione : prenotazioni) {
            LocalDate data = prenotazione.getData();
            String servizi = descriviServizi(prenotazione);
            StatoPrenotazione stato = prenotazione.getStato();

            righe.add(Map.of(
                    "data", (data != null) ? data.format(FORMATO_DATA_PRENOTAZIONE) : "",
                    "postazione", descriviPostazione(prenotazione),
                    "servizi", servizi.isEmpty() ? "nessuno" : servizi,
                    "stato", (stato != null) ? stato.nome() : "",
                    "prezzo", String.valueOf(prenotazione.getPrezzoTotale()),
                    "id", String.valueOf(prenotazione.getId()),
                    "annullabile", String.valueOf(prenotazione.isAnnullabile(oggi))));
        }

        return righe;
    }

    /*
     * Postazione scelta di una prenotazione ("Ombrellone n. X (Fila Y)"), oppure
     * stringa vuota se non determinabile.
     */
    private static String descriviPostazione(Prenotazione prenotazione) {
        Ombrellone ombrellone = prenotazione.getOmbrellone();
        if (ombrellone == null) {
            return "";
        }

        StringBuilder postazione = new StringBuilder();
        postazione.append("Ombrellone n. ").append(ombrellone.getNumero());
        if (ombrellone.getFila() != null) {
            postazione.append(" (Fila ").append(ombrellone.getFila().getNumero()).append(')');
        }

        return postazione.toString();
    }

    /*
     * Servizi aggiuntivi di una prenotazione, con quantità ("2 x Lettino,
     * 1 x Cabina"), o stringa vuota se non ci sono servizi.
     */
    private static String descriviServizi(Prenotazione prenotazione) {
        StringBuilder servizi = new StringBuilder();

        for (Map.Entry<ServizioAggiuntivo, Integer> voce : prenotazione.getQuantitaServizi().entrySet()) {
            if (servizi.length() > 0) {
                servizi.append(", ");
            }
            servizi.append(voce.getValue()).append(" x ").append(voce.getKey().getDescrizione());
        }

        return servizi.toString();
    }

    /*
     * Verifica che la prenotazione appartenga al cliente indicato
     */
    private static boolean appartieneA(Prenotazione prenotazione, Cliente cliente) {
        return prenotazione.getCliente() != null
                && prenotazione.getCliente().getId() != null
                && prenotazione.getCliente().getId().equals(cliente.getId());
    }

    /*
     * NOTIFICA
     */

    /*
     * Body della notifica di conferma/annullamento.
     * Il Boundary li richiede dopo l'esito OK e li passa al proprio Adapter.
     */
    public static String getMessaggioNotificaPrenotazione(String emailCliente, long idOmbrellone,
                                                        LocalDate data, long[] idServizi, int[] quantita) {
        Cliente cliente = GestoreUtenti.trovaCliente(emailCliente);
        Ombrellone ombrellone = GestoreStabilimento.trovaOmbrellone(idOmbrellone);
        if (cliente == null || ombrellone == null) {
            return null;
        }

        Map<ServizioAggiuntivo, Integer> quantitaServizi = new LinkedHashMap<>();
        if (idServizi != null && quantita != null && idServizi.length == quantita.length) {
            for (int i = 0; i < idServizi.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio = GestoreStabilimento.trovaServizio(idServizi[i]);
                if (servizio != null) {
                    quantitaServizi.put(servizio, quantita[i]);
                }
            }
        }

        double totale = getPrezzoTotale(idOmbrellone, idServizi, quantita, data);
        Prenotazione prenotazione = new Prenotazione(data, ombrellone, null, cliente,
                quantitaServizi, null, totale);
        return componiCorpoConferma(prenotazione);
    }

    public static String getMessaggioNotificaAnnullamento(String emailCliente, long idPrenotazione) {
        Cliente cliente = GestoreUtenti.trovaCliente(emailCliente);
        if (cliente == null) {
            return null;
        }
        Prenotazione prenotazione = new RegistroPrenotazioni().trovaPrenotazione(idPrenotazione);
        if (prenotazione == null || !appartieneA(prenotazione, cliente)) {
            return null;
        }
        return componiCorpoAnnullamento(prenotazione);
    }

    private static final DateTimeFormatter FORMATO_DATA_NOTIFICA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");


    /*
     * Testo della conferma.
     */
    private static String componiCorpoConferma(Prenotazione prenotazione) {
        StringBuilder corpo = new StringBuilder();
        intestazione(corpo, prenotazione.getCliente());
        corpo.append("la sua prenotazione è confermata.\n");
        corpo.append("Ombrellone n. ").append(numeroOmbrellone(prenotazione)).append('\n');
        if (prenotazione.getData() != null) {
            corpo.append("Data: ").append(prenotazione.getData().format(FORMATO_DATA_NOTIFICA)).append('\n');
        }

        Map<ServizioAggiuntivo, Integer> quantitaServizi = prenotazione.getQuantitaServizi();
        if (quantitaServizi != null && !quantitaServizi.isEmpty()) {
            corpo.append("Servizi aggiuntivi:\n");
            for (Map.Entry<ServizioAggiuntivo, Integer> voce : quantitaServizi.entrySet()) {
                int q = voce.getValue() != null ? voce.getValue() : 0;
                corpo.append("  - ").append(q).append(" x ")
                        .append(voce.getKey().getDescrizione()).append('\n');
            }
        }

        corpo.append("Totale: ").append(String.format("€ %.2f", prenotazione.getPrezzoTotale())).append('\n');
        return corpo.toString();
    }

    /*
     * Testo dell'annullamento.
     */
    private static String componiCorpoAnnullamento(Prenotazione prenotazione) {
        StringBuilder corpo = new StringBuilder();
        intestazione(corpo, prenotazione.getCliente());
        corpo.append("la sua prenotazione è stata annullata.\n");
        corpo.append("Ombrellone n. ").append(numeroOmbrellone(prenotazione)).append('\n');
        if (prenotazione.getData() != null) {
            corpo.append("Data: ").append(prenotazione.getData().format(FORMATO_DATA_NOTIFICA)).append('\n');
        }
        return corpo.toString();
    }

    /*
     * Riga di saluto iniziale.
     */
    private static void intestazione(StringBuilder corpo, Cliente cliente) {
        if (cliente != null && cliente.getNome() != null) {
            corpo.append("Gentile ").append(cliente.getNome())
                    .append(' ').append(cliente.getCognome()).append(",\n");
        }
    }

    private static int numeroOmbrellone(Prenotazione prenotazione) {
        return prenotazione.getOmbrellone() != null ? prenotazione.getOmbrellone().getNumero() : 0;
    }
}

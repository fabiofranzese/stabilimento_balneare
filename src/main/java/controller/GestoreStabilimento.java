package controller;

import database.GestorePersistenza;
import entity.Cliente;
import entity.FilaOmbrelloni;
import entity.Ombrellone;
import entity.OmbrelloneNonDisponibileException;
import entity.Prenotazione;
import entity.RegistroOmbrelloni;
import entity.RegistroPrenotazioni;
import entity.RegistroServiziAggiuntivi;
import entity.RegistroTariffe;
import entity.RegistroUtenti;
import entity.ServizioAggiuntivo;
import entity.ServizioEsauritoException;
import entity.Stagione;
import entity.StatoPrenotazione;
import entity.TariffaServizioAggiuntivo;
import entity.TariffaTipoFila;
import entity.TipoFila;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * GestoreStabilimento è il Controller (GRASP) e la Façade dei casi d'uso del
 * Gestore. Per ora copre la Configurazione stabilimento e la Definizione tariffe;
 * le operazioni di prenotazione/monitoraggio previste dal modello verranno
 * aggiunte qui nei casi d'uso successivi.
 *
 * Come GestoreUtenti, espone operazioni a grana grossa e scambia con il Boundary
 * solo tipi primitivi (int, String[], int[], double[]): il Boundary non conosce
 * le Entity (FilaOmbrelloni, Ombrellone, ServizioAggiuntivo, TipoFila, Tariffa,
 * Stagione) e non importa il package entity, rispettando la separazione BCED.
 *
 * NOTE: il modello prevede metodi a grana fine (aggiungiFila(tipoFila),
 * aggiungiOmbrellone(fila, numero)); esporli al Boundary farebbe però transitare
 * un'Entity (la fila) verso la GUI, violando BCED. Si usa quindi un'unica
 * operazione salvaConfigurazione: la Façade compone internamente le file e i
 * loro ombrelloni tramite FilaOmbrelloni.creaOmbrellone.
 */
public class GestoreStabilimento {

    // Formato di data per le righe dell'elenco prenotazioni (Gestione personali).
    private static final DateTimeFormatter FORMATO_DATA_PRENOTAZIONE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Esiti della configurazione.
    public static final int CONFIGURAZIONE_OK = 0;
    public static final int DATI_NON_VALIDI = 1;
    public static final int ERRORE_CONFIGURAZIONE = 2;
    public static final int PRENOTAZIONI_PRESENTI = 3;

    // Esiti della definizione tariffe.
    public static final int TARIFFE_OK = 10;
    public static final int TARIFFA_NON_VALIDA = 11;
    public static final int ERRORE_TARIFFE = 12;

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

    /*
     * Etichette in italiano della posizione di ogni fila, derivate dall'ordine:
     * con numeroFile file restituisce, per ogni indice, l'etichetta di
     * TipoFila.perPosizione (prima / intermedia / ultima). Il Boundary la usa per
     * mostrare la posizione assegnata dal sistema, che si aggiorna a ogni
     * aggiunta/rimozione di una fila. La regola resta nel dominio (TipoFila).
     */
    public static String[] etichettePosizioniFile(int numeroFile) {
        String[] etichette = new String[Math.max(0, numeroFile)];

        for (int i = 0; i < etichette.length; i++) {
            etichette[i] = TipoFila.perPosizione(i, numeroFile).getEtichetta();
        }

        return etichette;
    }

    /*
     * Caso d'uso Configurazione stabilimento.
     *
     * Riceve la disposizione (per ogni fila: numero di ombrelloni) e i servizi
     * aggiuntivi (per ognuno: descrizione + capacità). Valida i dati, poi rigenera
     * disposizione e servizi (strategia "replace"). La posizione delle file non è
     * scelta dal gestore: è derivata dall'ordine in RegistroOmbrelloni.
     *
     * Gli array dei servizi (descrizioniServizi, capacitaServizi) sono paralleli.
     */
    public static int salvaConfigurazione(int[] ombrelloniPerFila,
                                          String[] descrizioniServizi, int[] capacitaServizi) {

        if (!datiValidi(ombrelloniPerFila, descrizioniServizi, capacitaServizi)) {
            return DATI_NON_VALIDI;
        }

        // Precondizione: la riconfigurazione è distruttiva (sostituisce file e
        // servizi). Se esiste anche una sola prenotazione attiva, le sue
        // postazioni/servizi non possono essere rimossi senza incoerenze: si
        // blocca l'operazione.
        if (new RegistroPrenotazioni().esistonoPrenotazioniAttive()) {
            return PRENOTAZIONI_PRESENTI;
        }

        try {
            new RegistroOmbrelloni().configuraDisposizione(ombrelloniPerFila);
            new RegistroServiziAggiuntivi().sostituisciServizi(descrizioniServizi, capacitaServizi);

            return CONFIGURAZIONE_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_CONFIGURAZIONE;
        }
    }

    /*
     * Indica se esiste almeno una prenotazione attiva. La riconfigurazione dello
     * stabilimento è distruttiva: il Boundary usa questa precondizione per non
     * aprire nemmeno la configurazione quando ci sono prenotazioni attive.
     */
    public static boolean prenotazioniAttivePresenti() {
        return new RegistroPrenotazioni().esistonoPrenotazioniAttive();
    }

    // --- Lettura della configurazione corrente (per precaricare il form) ---
    // Si restituiscono solo tipi primitivi/array: nessuna Entity verso il Boundary.

    public static int[] ombrelloniPerFilaCorrenti() {
        List<FilaOmbrelloni> file = new RegistroOmbrelloni().getFile();
        int[] conteggi = new int[file.size()];

        for (int i = 0; i < file.size(); i++) {
            conteggi[i] = file.get(i).getOmbrelloni().size();
        }

        return conteggi;
    }

    public static String[] descrizioniServiziCorrenti() {
        List<ServizioAggiuntivo> servizi = new RegistroServiziAggiuntivi().getServizi();
        String[] descrizioni = new String[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            descrizioni[i] = servizi.get(i).getDescrizione();
        }

        return descrizioni;
    }

    public static int[] capacitaServiziCorrenti() {
        List<ServizioAggiuntivo> servizi = new RegistroServiziAggiuntivi().getServizi();
        int[] capacita = new int[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            capacita[i] = servizi.get(i).getCapacita();
        }

        return capacita;
    }

    /*
     * Validazione dei dati di configurazione:
     * - almeno una fila, ciascuna con almeno un ombrellone;
     * - array dei servizi coerenti in lunghezza; ogni descrizione non vuota;
     *   ogni capacità non negativa.
     */
    private static boolean datiValidi(int[] ombrelloniPerFila,
                                      String[] descrizioniServizi, int[] capacitaServizi) {

        if (ombrelloniPerFila == null
                || descrizioniServizi == null || capacitaServizi == null) {
            return false;
        }

        if (ombrelloniPerFila.length == 0) {
            return false;
        }

        for (int i = 0; i < ombrelloniPerFila.length; i++) {
            if (ombrelloniPerFila[i] < 1) {
                return false;
            }
        }

        if (descrizioniServizi.length != capacitaServizi.length) {
            return false;
        }

        for (int i = 0; i < descrizioniServizi.length; i++) {
            if (descrizioniServizi[i] == null || descrizioniServizi[i].trim().isEmpty()) {
                return false;
            }
            if (capacitaServizi[i] < 0) {
                return false;
            }
        }

        return true;
    }

    // ===== Definizione tariffe =====

    /*
     * Etichette in italiano delle stagioni, nell'ordine dell'enum Stagione:
     * popolano la combo del form (l'indice scelto corrisponde a
     * Stagione.values()[indice]).
     */
    public static String[] stagioni() {
        Stagione[] valori = Stagione.values();
        String[] etichette = new String[valori.length];

        for (int i = 0; i < valori.length; i++) {
            etichette[i] = valori[i].getEtichetta();
        }

        return etichette;
    }

    /*
     * Indica se lo stabilimento è già stato configurato (esiste almeno una fila).
     * La definizione delle tariffe è disponibile solo in tal caso: senza file non
     * ci sono postazioni da tariffare.
     */
    public static boolean configurazioneEffettuata() {
        return !new RegistroOmbrelloni().getFile().isEmpty();
    }

    /*
     * Etichette dei possibili "elementi" di una tariffa, per la combo del form:
     * prima i tipi di fila effettivamente configurati (ombrelloni), poi i servizi
     * aggiuntivi esistenti. L'indice scelto identifica l'elemento: 0..numTipi-1
     * sono i tipi di fila configurati, da numTipi in poi i servizi.
     */
    public static String[] elementiTariffa() {
        List<TipoFila> tipi = tipiFilaConfigurati();
        List<ServizioAggiuntivo> servizi = serviziOrdinati();

        String[] etichette = new String[tipi.size() + servizi.size()];

        for (int i = 0; i < tipi.size(); i++) {
            etichette[i] = "Ombrellone — " + tipi.get(i).getEtichetta();
        }
        for (int i = 0; i < servizi.size(); i++) {
            etichette[tipi.size() + i] = "Servizio — " + servizi.get(i).getDescrizione();
        }

        return etichette;
    }

    /*
     * Caso d'uso Definizione tariffe.
     *
     * Per ogni tariffa indicata (elemento + stagione + costo) imposta o aggiorna
     * il prezzo (strategia "upsert" nel RegistroTariffe). Gli array sono paralleli.
     */
    public static int salvaTariffe(int[] elementiIndici, int[] stagioniIndici, double[] costi) {
        List<TipoFila> tipi = tipiFilaConfigurati();
        List<ServizioAggiuntivo> servizi = serviziOrdinati();
        int numTipi = tipi.size();

        if (!datiTariffeValidi(elementiIndici, stagioniIndici, costi, numTipi + servizi.size())) {
            return TARIFFA_NON_VALIDA;
        }

        try {
            RegistroTariffe registroTariffe = new RegistroTariffe();
            Stagione[] stagioni = Stagione.values();

            for (int i = 0; i < elementiIndici.length; i++) {
                int elemento = elementiIndici[i];
                Stagione stagione = stagioni[stagioniIndici[i]];
                double costo = costi[i];

                if (elemento < numTipi) {
                    registroTariffe.definisciTariffaTipoFila(tipi.get(elemento), stagione, costo);
                } else {
                    registroTariffe.definisciTariffaServizio(servizi.get(elemento - numTipi), stagione, costo);
                }
            }

            return TARIFFE_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_TARIFFE;
        }
    }

    // --- Lettura delle tariffe correnti (per precaricare il form) ---
    // I tre metodi restituiscono array paralleli, allineati dallo stesso ordine.

    public static int[] elementiTariffeCorrenti() {
        List<double[]> righe = righeTariffe();
        int[] elementi = new int[righe.size()];

        for (int i = 0; i < righe.size(); i++) {
            elementi[i] = (int) righe.get(i)[0];
        }

        return elementi;
    }

    public static int[] stagioniTariffeCorrenti() {
        List<double[]> righe = righeTariffe();
        int[] stagioni = new int[righe.size()];

        for (int i = 0; i < righe.size(); i++) {
            stagioni[i] = (int) righe.get(i)[1];
        }

        return stagioni;
    }

    public static double[] costiTariffeCorrenti() {
        List<double[]> righe = righeTariffe();
        double[] costi = new double[righe.size()];

        for (int i = 0; i < righe.size(); i++) {
            costi[i] = righe.get(i)[2];
        }

        return costi;
    }

    /*
     * Validazione delle tariffe: array coerenti in lunghezza e non vuoti, ogni
     * indice valido, e ogni costo strettamente positivo (flusso: tariffa <= 0
     * è un errore).
     */
    private static boolean datiTariffeValidi(int[] elementiIndici, int[] stagioniIndici,
                                             double[] costi, int numeroElementi) {

        if (elementiIndici == null || stagioniIndici == null || costi == null) {
            return false;
        }

        if (elementiIndici.length == 0
                || elementiIndici.length != stagioniIndici.length
                || elementiIndici.length != costi.length) {
            return false;
        }

        int numeroStagioni = Stagione.values().length;
        for (int i = 0; i < elementiIndici.length; i++) {
            if (elementiIndici[i] < 0 || elementiIndici[i] >= numeroElementi) {
                return false;
            }
            if (stagioniIndici[i] < 0 || stagioniIndici[i] >= numeroStagioni) {
                return false;
            }
            if (costi[i] <= 0) {
                return false;
            }
        }

        return true;
    }

    /*
     * Costruisce le righe delle tariffe correnti come array {elemento, stagione,
     * costo}, ordinate in modo deterministico: così i tre metodi di lettura
     * restituiscono array paralleli allineati anche se le query cambiano ordine.
     */
    private static List<double[]> righeTariffe() {
        List<TipoFila> tipi = tipiFilaConfigurati();
        int numTipi = tipi.size();
        List<ServizioAggiuntivo> servizi = serviziOrdinati();
        RegistroTariffe registroTariffe = new RegistroTariffe();
        List<double[]> righe = new ArrayList<>();

        for (TariffaTipoFila tariffa : registroTariffe.getTariffeTipoFila()) {
            int posizione = tipi.indexOf(tariffa.getTipoFila());
            if (posizione >= 0) {
                righe.add(new double[]{
                        posizione,
                        tariffa.getStagione().ordinal(),
                        tariffa.getCosto()});
            }
        }

        for (TariffaServizioAggiuntivo tariffa : registroTariffe.getTariffeServizio()) {
            int posizione = indiceServizio(servizi, tariffa.getServizio());
            if (posizione >= 0) {
                righe.add(new double[]{
                        numTipi + posizione,
                        tariffa.getStagione().ordinal(),
                        tariffa.getCosto()});
            }
        }

        righe.sort((a, b) -> a[0] != b[0]
                ? Double.compare(a[0], b[0])
                : Double.compare(a[1], b[1]));

        return righe;
    }

    /*
     * Tipi di fila effettivamente configurati (presenti in almeno una fila), in
     * ordine stabile (ordinale dell'enum). Su questo elenco si basa la codifica
     * degli indici dei tipi di fila come elementi tariffabili: si possono definire
     * tariffe solo per i tipi di fila che esistono nello stabilimento.
     */
    private static List<TipoFila> tipiFilaConfigurati() {
        List<FilaOmbrelloni> file = new RegistroOmbrelloni().getFile();
        List<TipoFila> tipi = new ArrayList<>();

        for (TipoFila tipo : TipoFila.values()) {
            for (FilaOmbrelloni fila : file) {
                if (fila.getTipoFila() == tipo) {
                    tipi.add(tipo);
                    break;
                }
            }
        }

        return tipi;
    }

    /*
     * Servizi aggiuntivi ordinati per id: ordinamento stabile su cui si basa la
     * codifica degli indici degli elementi (combo, salvataggio, precaricamento).
     */
    private static List<ServizioAggiuntivo> serviziOrdinati() {
        List<ServizioAggiuntivo> servizi = new RegistroServiziAggiuntivi().getServizi();
        servizi.sort(Comparator.comparing(ServizioAggiuntivo::getId));
        return servizi;
    }

    private static int indiceServizio(List<ServizioAggiuntivo> servizi, ServizioAggiuntivo servizio) {
        for (int i = 0; i < servizi.size(); i++) {
            if (servizi.get(i).getId().equals(servizio.getId())) {
                return i;
            }
        }
        return -1;
    }

    // ===== Visualizzazione mappa =====
    //
    // Realizzazione, sicura rispetto a BCED, di visualizzaDisponibilita(data): il
    // Boundary riceve solo array di primitivi/etichette, allineati per indice di
    // fila e poi per indice di ombrellone. Le file sono ordinate per posizione
    // (prima → intermedia → ultima, poi per numero): così la mappa va dalla riva
    // verso l'interno e il lato mare è quello delle prime file. Nessuna Entity
    // verso la GUI.

    /*
     * Etichette delle file: "Fila N". La posizione (prima/intermedia/ultima) non
     * è mostrata: è suggerita dall'ordine delle file rispetto al mare.
     */
    public static String[] etichetteFile() {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        String[] etichette = new String[file.size()];

        for (int i = 0; i < file.size(); i++) {
            etichette[i] = "Fila " + file.get(i).getNumero();
        }

        return etichette;
    }

    /*
     * Numeri degli ombrelloni, per fila (array 2D irregolare).
     */
    public static int[][] numeriOmbrelloniPerFila() {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        int[][] numeri = new int[file.size()][];

        for (int i = 0; i < file.size(); i++) {
            List<Ombrellone> ombrelloni = ombrelloniOrdinati(file.get(i));
            numeri[i] = new int[ombrelloni.size()];
            for (int j = 0; j < ombrelloni.size(); j++) {
                numeri[i][j] = ombrelloni.get(j).getNumero();
            }
        }

        return numeri;
    }

    /*
     * Id degli ombrelloni, per fila: identificano l'ombrellone selezionato (usati
     * per avviare la prenotazione nel caso d'uso successivo).
     */
    public static long[][] idOmbrelloniPerFila() {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        long[][] identificativi = new long[file.size()][];

        for (int i = 0; i < file.size(); i++) {
            List<Ombrellone> ombrelloni = ombrelloniOrdinati(file.get(i));
            identificativi[i] = new long[ombrelloni.size()];
            for (int j = 0; j < ombrelloni.size(); j++) {
                identificativi[i][j] = ombrelloni.get(j).getId();
            }
        }

        return identificativi;
    }

    /*
     * Disponibilità degli ombrelloni in una data, per fila: true = libero,
     * false = occupato (dato derivato dalle prenotazioni attive).
     */
    public static boolean[][] disponibilitaPerFila(LocalDate data) {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        boolean[][] disponibili = new boolean[file.size()][];

        for (int i = 0; i < file.size(); i++) {
            List<Ombrellone> ombrelloni = ombrelloniOrdinati(file.get(i));
            disponibili[i] = new boolean[ombrelloni.size()];
            for (int j = 0; j < ombrelloni.size(); j++) {
                disponibili[i][j] = !registroPrenotazioni.isOmbrelloneOccupato(ombrelloni.get(j), data);
            }
        }

        return disponibili;
    }

    /*
     * Prezzo del tipo di fila indicato per la stagione in cui cade la data scelta
     * (condiviso da tutti gli ombrelloni della fila). Vale -1 se per quella
     * stagione la tariffa non è definita.
     */
    public static double prezzoFila(int indiceFila, LocalDate data) {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        if (indiceFila < 0 || indiceFila >= file.size()) {
            return -1;
        }

        TipoFila tipo = file.get(indiceFila).getTipoFila();
        Stagione stagione = Stagione.perData(data);

        for (TariffaTipoFila tariffa : new RegistroTariffe().getTariffeTipoFila()) {
            if (tariffa.getTipoFila() == tipo && tariffa.getStagione() == stagione) {
                return tariffa.getCosto();
            }
        }

        return -1;
    }

    /*
     * Nome della stagione in cui cade la data scelta (per la GUI).
     */
    public static String nomeStagione(LocalDate data) {
        return Stagione.perData(data).getEtichetta();
    }

    /*
     * File ordinate per la mappa: per posizione (prima → intermedia → ultima),
     * poi per numero. L'ordine riflette la distanza dal mare.
     */
    private static List<FilaOmbrelloni> fileOrdinatePerMappa() {
        List<FilaOmbrelloni> file = new RegistroOmbrelloni().getFile();
        file.sort(Comparator.comparingInt((FilaOmbrelloni f) -> f.getTipoFila().ordinal())
                .thenComparingInt(FilaOmbrelloni::getNumero));
        return file;
    }

    private static List<Ombrellone> ombrelloniOrdinati(FilaOmbrelloni fila) {
        List<Ombrellone> ombrelloni = new ArrayList<>(fila.getOmbrelloni());
        ombrelloni.sort(Comparator.comparingInt(Ombrellone::getNumero));
        return ombrelloni;
    }

    // ===== Effettua prenotazione =====
    //
    // Operazioni del caso d'uso Effettua Prenotazione (estensione di
    // Visualizzazione Mappa). Il Boundary identifica cliente, ombrellone e servizi
    // con valori semplici (email, id): nessuna Entity attraversa il confine B/C.

    /*
     * Servizi aggiuntivi che il cliente può prenotare per la data scelta: quelli
     * con disponibilità residua positiva E con una tariffa definita per la stagione
     * di quella data. I servizi esauriti o senza tariffa non sono mostrati. Su
     * questo elenco (ordine stabile) si allineano descrizioniServizi(data),
     * idServizi(data), residuoServizi(data) e prezziServizi(data).
     */
    private static List<ServizioAggiuntivo> serviziSelezionabili(LocalDate data) {
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        Stagione stagione = Stagione.perData(data);
        List<ServizioAggiuntivo> selezionabili = new ArrayList<>();

        for (ServizioAggiuntivo servizio : serviziOrdinati()) {
            boolean disponibile = registroPrenotazioni.residuoServizio(servizio, data) > 0;
            boolean tariffato = costoServizio(servizio, stagione) >= 0;
            if (disponibile && tariffato) {
                selezionabili.add(servizio);
            }
        }

        return selezionabili;
    }

    /*
     * Descrizioni dei servizi prenotabili per la data: descrizioniServizi(data)[i],
     * idServizi(data)[i], residuoServizi(data)[i] e prezziServizi(data)[i]
     * descrivono lo stesso servizio.
     */
    public static String[] descrizioniServizi(LocalDate data) {
        List<ServizioAggiuntivo> servizi = serviziSelezionabili(data);
        String[] descrizioni = new String[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            descrizioni[i] = servizi.get(i).getDescrizione();
        }

        return descrizioni;
    }

    /*
     * Id dei servizi prenotabili per la data, allineati a descrizioniServizi(data):
     * identificano il servizio scelto nella prenotazione.
     */
    public static long[] idServizi(LocalDate data) {
        List<ServizioAggiuntivo> servizi = serviziSelezionabili(data);
        long[] identificativi = new long[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            identificativi[i] = servizi.get(i).getId();
        }

        return identificativi;
    }

    /*
     * Disponibilità residua di ciascun servizio prenotabile per la data (dato
     * derivato dalle prenotazioni attive). È la quantità massima ordinabile.
     */
    public static int[] residuoServizi(LocalDate data) {
        List<ServizioAggiuntivo> servizi = serviziSelezionabili(data);
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        int[] residui = new int[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            residui[i] = registroPrenotazioni.residuoServizio(servizi.get(i), data);
        }

        return residui;
    }

    /*
     * Prezzo unitario di ciascun servizio prenotabile per la stagione della data.
     * Allineato a descrizioniServizi(data).
     */
    public static double[] prezziServizi(LocalDate data) {
        List<ServizioAggiuntivo> servizi = serviziSelezionabili(data);
        Stagione stagione = Stagione.perData(data);
        double[] prezzi = new double[servizi.size()];

        for (int i = 0; i < servizi.size(); i++) {
            prezzi[i] = costoServizio(servizi.get(i), stagione);
        }

        return prezzi;
    }

    /*
     * Prezzo dell'ombrellone (in base al tipo della sua fila) per la stagione in
     * cui cade la data; -1 se la tariffa non è definita o l'ombrellone non esiste.
     */
    public static double prezzoOmbrellone(long idOmbrellone, LocalDate data) {
        Ombrellone ombrellone = new GestorePersistenza().trovaPerId(Ombrellone.class, idOmbrellone);

        if (ombrellone == null || ombrellone.getFila() == null) {
            return -1;
        }

        return costoTipoFila(ombrellone.getFila().getTipoFila(), Stagione.perData(data));
    }

    /*
     * Prezzo totale della prenotazione: ombrellone + servizi selezionati per le
     * rispettive quantità, alla stagione della data. Gli array dei servizi
     * (idServiziScelti, quantita) sono paralleli. Le tariffe non definite (-1) non
     * incidono sul totale.
     */
    public static double prezzoTotale(long idOmbrellone, long[] idServiziScelti,
                                      int[] quantita, LocalDate data) {
        double totale = Math.max(0, prezzoOmbrellone(idOmbrellone, data));

        if (idServiziScelti != null && quantita != null
                && idServiziScelti.length == quantita.length) {
            Stagione stagione = Stagione.perData(data);
            for (int i = 0; i < idServiziScelti.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio =
                        new GestorePersistenza().trovaPerId(ServizioAggiuntivo.class, idServiziScelti[i]);
                if (servizio != null) {
                    totale += Math.max(0, costoServizio(servizio, stagione)) * quantita[i];
                }
            }
        }

        return totale;
    }

    /*
     * Caso d'uso Effettua Prenotazione.
     *
     * Risolve cliente (per email) e ombrellone (per id) e costruisce la mappa
     * servizio→quantità (solo le quantità positive), poi delega al
     * RegistroPrenotazioni la creazione con controllo dei conflitti. Traduce le
     * eccezioni di dominio nei codici di esito per il Boundary. Gli array
     * idServiziScelti e quantita sono paralleli.
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

        GestorePersistenza gestorePersistenza = new GestorePersistenza();

        Cliente cliente = clientePerEmail(emailCliente);
        Ombrellone ombrellone = gestorePersistenza.trovaPerId(Ombrellone.class, idOmbrellone);

        if (cliente == null || ombrellone == null) {
            return DATI_PRENOTAZIONE_NON_VALIDI;
        }

        Map<ServizioAggiuntivo, Integer> quantitaServizi = new LinkedHashMap<>();
        if (idServiziScelti != null) {
            for (int i = 0; i < idServiziScelti.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio =
                        gestorePersistenza.trovaPerId(ServizioAggiuntivo.class, idServiziScelti[i]);
                if (servizio == null) {
                    return DATI_PRENOTAZIONE_NON_VALIDI;
                }
                quantitaServizi.put(servizio, quantita[i]);
            }
        }

        // Prezzo "congelato" al momento della prenotazione, alle tariffe vigenti.
        double totale = prezzoTotale(idOmbrellone, idServiziScelti, quantita, data);

        try {
            new RegistroPrenotazioni()
                    .effettuaPrenotazione(cliente, ombrellone, data, quantitaServizi, totale);
            return PRENOTAZIONE_OK;

        } catch (OmbrelloneNonDisponibileException e) {
            return OMBRELLONE_NON_DISPONIBILE;
        } catch (ServizioEsauritoException e) {
            return SERVIZIO_ESAURITO;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_PRENOTAZIONE;
        }
    }

    // --- Helper di prezzo / risoluzione ---

    private static Cliente clientePerEmail(String email) {
        return new RegistroUtenti().cercaUtentePerEmail(email) instanceof Cliente cliente
                ? cliente
                : null;
    }

    private static double costoTipoFila(TipoFila tipo, Stagione stagione) {
        for (TariffaTipoFila tariffa : new RegistroTariffe().getTariffeTipoFila()) {
            if (tariffa.getTipoFila() == tipo && tariffa.getStagione() == stagione) {
                return tariffa.getCosto();
            }
        }
        return -1;
    }

    private static double costoServizio(ServizioAggiuntivo servizio, Stagione stagione) {
        for (TariffaServizioAggiuntivo tariffa : new RegistroTariffe().getTariffeServizio()) {
            if (tariffa.getServizio().getId().equals(servizio.getId())
                    && tariffa.getStagione() == stagione) {
                return tariffa.getCosto();
            }
        }
        return -1;
    }

    // ===== Gestione prenotazioni personali =====
    //
    // Operazioni del caso d'uso Gestione prenotazioni personali (attore
    // ClienteAutenticato): consultazione dello storico delle proprie prenotazioni
    // e annullamento entro il limite temporale. Il Boundary identifica il cliente
    // con l'email e la prenotazione con il suo id: nessuna Entity attraversa il
    // confine B/C. I metodi di lettura restituiscono array paralleli, allineati
    // dallo stesso ordinamento (prenotazioniOrdinate): l'elenco mostra solo la
    // data, mentre i campi di dettaglio (postazione, servizi, stato, prezzo) sono
    // mostrati per la prenotazione selezionata.

    /*
     * Date delle prenotazioni del cliente (per l'elenco nel form: una voce per
     * prenotazione). Allineata per indice a tutti gli altri array di lettura.
     */
    public static String[] datePrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        String[] date = new String[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            LocalDate data = prenotazioni.get(i).getData();
            date[i] = (data != null) ? data.format(FORMATO_DATA_PRENOTAZIONE) : "";
        }

        return date;
    }

    /*
     * Postazione scelta di ciascuna prenotazione del cliente ("Ombrellone n. X
     * (Fila Y)"), per il dettaglio. Allineata a datePrenotazioniCliente.
     */
    public static String[] postazioniPrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        String[] postazioni = new String[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            postazioni[i] = descriviPostazione(prenotazioni.get(i));
        }

        return postazioni;
    }

    /*
     * Servizi aggiuntivi (con quantità) di ciascuna prenotazione del cliente,
     * "nessuno" se non ce ne sono. Per il dettaglio; allineata a
     * datePrenotazioniCliente.
     */
    public static String[] serviziPrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        String[] servizi = new String[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            String descrizione = descriviServizi(prenotazioni.get(i));
            servizi[i] = descrizione.isEmpty() ? "nessuno" : descrizione;
        }

        return servizi;
    }

    /*
     * Stato di ciascuna prenotazione del cliente ("Prenotata"/"Annullata"), per il
     * dettaglio. Allineata a datePrenotazioniCliente.
     */
    public static String[] statiPrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        String[] stati = new String[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            StatoPrenotazione stato = prenotazioni.get(i).getStato();
            stati[i] = (stato != null) ? stato.nome() : "";
        }

        return stati;
    }

    /*
     * Prezzo totale "congelato" di ciascuna prenotazione del cliente, per il
     * dettaglio. Allineato a datePrenotazioniCliente.
     */
    public static double[] prezziPrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        double[] prezzi = new double[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            prezzi[i] = prenotazioni.get(i).getPrezzoTotale();
        }

        return prezzi;
    }

    /*
     * Id delle prenotazioni del cliente, allineati a datePrenotazioniCliente:
     * identificano la prenotazione selezionata per l'annullamento.
     */
    public static long[] idPrenotazioniCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        long[] identificativi = new long[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            identificativi[i] = prenotazioni.get(i).getId();
        }

        return identificativi;
    }

    /*
     * Per ogni prenotazione del cliente, indica se è annullabile alla data odierna
     * (stato Prenotata e richiesta entro il limite temporale: oggi < data).
     * Allineata a datePrenotazioniCliente: il Boundary la usa per
     * abilitare/disabilitare l'annullamento sulla prenotazione selezionata.
     */
    public static boolean[] annullabiliCliente(String emailCliente) {
        List<Prenotazione> prenotazioni = prenotazioniOrdinate(emailCliente);
        LocalDate oggi = LocalDate.now();
        boolean[] annullabili = new boolean[prenotazioni.size()];

        for (int i = 0; i < prenotazioni.size(); i++) {
            annullabili[i] = prenotazioni.get(i).isAnnullabile(oggi);
        }

        return annullabili;
    }

    /*
     * Caso d'uso Gestione prenotazioni personali — Annullamento prenotazione.
     *
     * Risolve cliente (per email) e prenotazione (per id), verifica che la
     * prenotazione appartenga a quel cliente (gestione dei permessi) e che sia
     * annullabile entro il limite temporale, poi delega al RegistroPrenotazioni la
     * transizione a Annullata, il salvataggio e la notifica. Restituisce un codice
     * di esito per il Boundary.
     */
    public static int annullaPrenotazione(String emailCliente, long idPrenotazione) {
        if (emailCliente == null) {
            return PRENOTAZIONE_NON_TROVATA;
        }

        Cliente cliente = clientePerEmail(emailCliente);
        if (cliente == null) {
            return PRENOTAZIONE_NON_TROVATA;
        }

        Prenotazione prenotazione =
                new GestorePersistenza().trovaPerId(Prenotazione.class, idPrenotazione);

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
     * Prenotazioni del cliente in ordine deterministico (per data, poi per id):
     * l'ordine stabile su cui si allineano gli array di lettura. Restituisce una
     * lista vuota se l'email non corrisponde a un cliente.
     */
    private static List<Prenotazione> prenotazioniOrdinate(String emailCliente) {
        Cliente cliente = clientePerEmail(emailCliente);
        if (cliente == null) {
            return new ArrayList<>();
        }

        List<Prenotazione> prenotazioni = new RegistroPrenotazioni().prenotazioniCliente(cliente);
        prenotazioni.sort(Comparator.comparing(Prenotazione::getData)
                .thenComparing(Prenotazione::getId));
        return prenotazioni;
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
     * Elenco dei servizi aggiuntivi di una prenotazione, con quantità
     * ("2 x Lettino, 1 x Cabina"); stringa vuota se non ci sono servizi.
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
     * Verifica che la prenotazione appartenga al cliente indicato (confronto per
     * id: gli oggetti provengono da caricamenti distinti).
     */
    private static boolean appartieneA(Prenotazione prenotazione, Cliente cliente) {
        return prenotazione.getCliente() != null
                && prenotazione.getCliente().getId() != null
                && prenotazione.getCliente().getId().equals(cliente.getId());
    }
}

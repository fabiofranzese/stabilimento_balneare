package controller;

import entity.Cliente;
import entity.FilaOmbrelloni;
import entity.Ombrellone;
import entity.Prenotazione;
import entity.RegistroOmbrelloni;
import entity.RegistroPrenotazioni;
import entity.RegistroServiziAggiuntivi;
import entity.RegistroTariffe;
import entity.RegistroUtenti;
import entity.ServizioAggiuntivo;
import entity.Stagione;
import entity.StatoPrenotazione;
import entity.TariffaServizioAggiuntivo;
import entity.TariffaTipoFila;
import entity.TipoFila;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * GestoreStabilimento è il Controller (GRASP) e la Façade dei casi d'uso che
 * ruotano attorno allo stabilimento: Configurazione stabilimento, Definizione
 * tariffe, Visualizzazione mappa, Effettua prenotazione e Gestione prenotazioni
 * personali.
 *
 * Come GestoreUtenti, espone operazioni a grana grossa e scambia con il Boundary
 * solo tipi primitivi, array e collezioni di stringhe del JDK (List/Map, con le
 * chiavi documentate sui singoli metodi): il Boundary non conosce le Entity e
 * non importa il package entity, nel rispetto della separazione BCED.
 *
 * NOTE: il modello prevede metodi a grana fine (aggiungiFila(tipoFila),
 * aggiungiOmbrellone(fila, numero)); esporli al Boundary farebbe però transitare
 * un'Entity (la fila) verso la GUI. Si usa quindi un'unica operazione
 * salvaConfigurazione: la Façade compone internamente le file e i loro
 * ombrelloni tramite FilaOmbrelloni.creaOmbrellone.
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
     * Etichette in italiano della posizione di ogni fila, derivate dall'ordine
     * (prima/intermedia/ultima). Il Boundary le usa per mostrare la posizione
     * assegnata dal sistema, aggiornata a ogni aggiunta/rimozione di una fila.
     */
    public static String[] getEtichettePosizioniFile(int numeroFile) {
        String[] etichette = new String[Math.max(0, numeroFile)];

        for (int i = 0; i < etichette.length; i++) {
            etichette[i] = tipoFilaPerPosizione(i, numeroFile).getEtichetta();
        }

        return etichette;
    }

    /*
     * Regola di derivazione della posizione: la posizione di una fila dipende
     * dal suo ordine nello stabilimento, non è scelta dal gestore. La prima è
     * PRIMA_FILA (anche se è l'unica: il primo controllo vince), l'ultima è
     * ULTIMA_FILA, quelle in mezzo FILA_INTERMEDIA.
     */
    private static TipoFila tipoFilaPerPosizione(int indice, int totale) {
        if (indice <= 0) {
            return TipoFila.PRIMA_FILA;
        }
        if (indice >= totale - 1) {
            return TipoFila.ULTIMA_FILA;
        }
        return TipoFila.FILA_INTERMEDIA;
    }

    /*
     * Caso d'uso Configurazione stabilimento.
     *
     * Riceve la disposizione (per ogni fila: numero di ombrelloni) e i servizi
     * aggiuntivi (descrizione + capacità, array paralleli). Valida i dati, poi
     * rigenera disposizione e servizi (strategia "replace"). La posizione delle
     * file non è scelta dal gestore: è derivata qui dall'ordine
     * (tipoFilaPerPosizione) e passata al RegistroOmbrelloni.
     */
    public static int salvaConfigurazione(int[] ombrelloniPerFila,
                                          String[] descrizioniServizi, int[] capacitaServizi) {

        if (!isDatiValidi(ombrelloniPerFila, descrizioniServizi, capacitaServizi)) {
            return DATI_NON_VALIDI;
        }

        // Precondizione: la riconfigurazione è distruttiva (sostituisce file e
        // servizi), quindi è bloccata se esiste anche una sola prenotazione attiva
        // che ne riferisce postazioni o servizi.
        if (new RegistroPrenotazioni().isPrenotazioniAttivePresenti()) {
            return PRENOTAZIONI_PRESENTI;
        }

        // Posizione di ciascuna fila, derivata dall'ordine.
        TipoFila[] tipiFile = new TipoFila[ombrelloniPerFila.length];
        for (int i = 0; i < tipiFile.length; i++) {
            tipiFile[i] = tipoFilaPerPosizione(i, tipiFile.length);
        }

        try {
            new RegistroOmbrelloni().configuraDisposizione(tipiFile, ombrelloniPerFila);
            new RegistroServiziAggiuntivi().sostituisciServizi(descrizioniServizi, capacitaServizi);

            return CONFIGURAZIONE_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_CONFIGURAZIONE;
        }
    }

    /*
     * Lettura delle file correnti, per precaricare il form (nessuna Entity verso
     * il Boundary): una riga per fila, nell'ordine della disposizione, con chiave
     *   "numeroOmbrelloni" — numero di ombrelloni della fila (intero come stringa).
     */
    public static List<Map<String, String>> getFileConfigurate() {
        List<Map<String, String>> righe = new ArrayList<>();

        for (FilaOmbrelloni fila : new RegistroOmbrelloni().getFile()) {
            righe.add(Map.of(
                    "numeroOmbrelloni", String.valueOf(fila.getOmbrelloni().size())));
        }

        return righe;
    }

    /*
     * Lettura dei servizi aggiuntivi correnti, per precaricare il form (nessuna
     * Entity verso il Boundary): una riga per servizio, con chiavi
     *   "descrizione" — descrizione del servizio;
     *   "capacita"    — capacità giornaliera (intero come stringa).
     */
    public static List<Map<String, String>> getServiziConfigurati() {
        List<Map<String, String>> righe = new ArrayList<>();

        for (ServizioAggiuntivo servizio : new RegistroServiziAggiuntivi().getServizi()) {
            righe.add(Map.of(
                    "descrizione", servizio.getDescrizione(),
                    "capacita", String.valueOf(servizio.getCapacita())));
        }

        return righe;
    }

    /*
     * Validazione della configurazione: almeno una fila, ciascuna con almeno un
     * ombrellone; array dei servizi coerenti in lunghezza, descrizioni non vuote,
     * capacità non negative.
     */
    private static boolean isDatiValidi(int[] ombrelloniPerFila,
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
    public static String[] getStagioni() {
        Stagione[] valori = Stagione.values();
        String[] etichette = new String[valori.length];

        for (int i = 0; i < valori.length; i++) {
            etichette[i] = valori[i].getEtichetta();
        }

        return etichette;
    }

    /*
     * Stagione in cui cade una data. Regola semplice (facilmente modificabile in
     * un unico punto): da giugno a settembre è alta stagione, altrimenti bassa.
     */
    private static Stagione stagionePerData(LocalDate data) {
        int mese = data.getMonthValue();
        return (mese >= 6 && mese <= 9) ? Stagione.ALTA : Stagione.BASSA;
    }

    /*
     * Indica se lo stabilimento è già stato configurato (esiste almeno una fila):
     * senza file non ci sono postazioni da tariffare.
     */
    public static boolean isConfigurazioneEffettuata() {
        return !new RegistroOmbrelloni().getFile().isEmpty();
    }

    /*
     * Etichette dei possibili "elementi" di una tariffa, per la combo del form:
     * prima i tipi di fila effettivamente configurati, poi i servizi aggiuntivi.
     * L'indice scelto identifica l'elemento: 0..numTipi-1 i tipi di fila, da
     * numTipi in poi i servizi.
     */
    public static String[] getElementiTariffa() {
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
     * Caso d'uso Definizione tariffe ("reconcile-on-save").
     *
     * La lista inviata (array paralleli elemento + stagione + costo) è lo stato
     * desiderato completo: ogni tariffa indicata viene impostata o aggiornata
     * ("upsert" nel RegistroTariffe), quelle non più presenti vengono eliminate
     * dal database. Una lista vuota è valida: "elimina tutte le tariffe".
     */
    public static int salvaTariffe(int[] elementiIndici, int[] stagioniIndici, double[] costi) {
        List<TipoFila> tipi = tipiFilaConfigurati();
        List<ServizioAggiuntivo> servizi = serviziOrdinati();
        int numTipi = tipi.size();

        if (!isDatiTariffeValidi(elementiIndici, stagioniIndici, costi, numTipi + servizi.size())) {
            return TARIFFA_NON_VALIDA;
        }

        try {
            RegistroTariffe registroTariffe = new RegistroTariffe();
            Stagione[] stagioni = Stagione.values();

            // Chiavi (elemento + stagione) inviate dal gestore: lo stato desiderato.
            Set<String> chiaviInviate = new HashSet<>();
            for (int i = 0; i < elementiIndici.length; i++) {
                chiaviInviate.add(chiaveTariffa(elementiIndici[i], stagioniIndici[i]));
            }

            // Elimina dal DB le tariffe non più presenti nella lista inviata.
            eliminaTariffeNonInviate(registroTariffe, tipi, servizi, numTipi, chiaviInviate);

            // Imposta o aggiorna le tariffe inviate.
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

    /*
     * Elimina le tariffe presenti nel database ma non nell'insieme inviato dal
     * gestore (chiavi elemento+stagione).
     *
     * NOTE: la riconciliazione è limitata agli elementi visibili nel form (tipi
     * di fila configurati + servizi esistenti). Una tariffa di un tipo/servizio
     * non più presente non compare nell'elenco, quindi non può essere stata
     * rimossa dal gestore: viene preservata in archivio, non eliminata.
     */
    private static void eliminaTariffeNonInviate(RegistroTariffe registroTariffe,
                                                 List<TipoFila> tipi, List<ServizioAggiuntivo> servizi,
                                                 int numTipi, Set<String> chiaviInviate) {

        for (TariffaTipoFila tariffa : registroTariffe.getTariffeTipoFila()) {
            int elemento = tipi.indexOf(tariffa.getTipoFila());
            if (elemento >= 0
                    && !chiaviInviate.contains(chiaveTariffa(elemento, tariffa.getStagione().ordinal()))) {
                registroTariffe.elimina(tariffa);
            }
        }

        for (TariffaServizioAggiuntivo tariffa : registroTariffe.getTariffeServizio()) {
            int posizione = indiceServizio(servizi, tariffa.getServizio());
            if (posizione >= 0
                    && !chiaviInviate.contains(chiaveTariffa(numTipi + posizione, tariffa.getStagione().ordinal()))) {
                registroTariffe.elimina(tariffa);
            }
        }
    }

    /*
     * Chiave univoca di una tariffa per la riconciliazione: indice dell'elemento
     * e ordinale della stagione.
     */
    private static String chiaveTariffa(int elemento, int stagioneOrdinale) {
        return elemento + ":" + stagioneOrdinale;
    }

    /*
     * Validazione delle tariffe: array coerenti in lunghezza, indici validi,
     * costi strettamente positivi (dal flusso: tariffa <= 0 è un errore). Una
     * lista vuota è valida: significa "elimina tutte le tariffe".
     */
    private static boolean isDatiTariffeValidi(int[] elementiIndici, int[] stagioniIndici,
                                               double[] costi, int numeroElementi) {

        if (elementiIndici == null || stagioniIndici == null || costi == null) {
            return false;
        }

        if (elementiIndici.length != stagioniIndici.length
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
     * Lettura delle tariffe correnti, per precaricare il form in un'unica
     * chiamata: una riga per tariffa, in ordine deterministico (per elemento,
     * poi per stagione) anche se le query cambiano ordine, con chiavi
     *   "elemento" — indice dell'elemento (vedi getElementiTariffa);
     *   "stagione" — indice della stagione (vedi getStagioni);
     *   "costo"    — costo della tariffa
     * (tutti i valori come stringhe). Nessuna Entity verso il Boundary.
     */
    public static List<Map<String, String>> getTariffeCorrenti() {
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

        List<Map<String, String>> tariffe = new ArrayList<>();
        for (double[] riga : righe) {
            tariffe.add(Map.of(
                    "elemento", String.valueOf((int) riga[0]),
                    "stagione", String.valueOf((int) riga[1]),
                    "costo", String.valueOf(riga[2])));
        }

        return tariffe;
    }

    /*
     * Tipi di fila effettivamente configurati (presenti in almeno una fila), in
     * ordine stabile (ordinale dell'enum): si possono tariffare solo i tipi di
     * fila che esistono nello stabilimento.
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
    // Realizzazione, sicura rispetto a BCED, di visualizzaDisponibilita(data):
    // il Boundary riceve solo etichette e righe di stringhe a chiavi, allineate
    // per indice di fila e poi di ombrellone. Le file sono ordinate per
    // posizione (prima → intermedia → ultima, poi per numero): la mappa va
    // dalla riva verso l'interno.

    /*
     * Etichette delle file: "Fila N". La posizione non è mostrata: è suggerita
     * dall'ordine delle file rispetto al mare.
     */
    public static String[] getEtichetteFile() {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        String[] etichette = new String[file.size()];

        for (int i = 0; i < file.size(); i++) {
            etichette[i] = "Fila " + file.get(i).getNumero();
        }

        return etichette;
    }

    /*
     * Mappa degli ombrelloni per la data scelta, in un'unica chiamata: una lista
     * per fila (stesso ordine di getEtichetteFile), con una riga per ombrellone
     * dalle chiavi
     *   "numero"      — numero dell'ombrellone;
     *   "id"          — id che identifica l'ombrellone selezionato sulla mappa
     *                   per avviarne la prenotazione;
     *   "disponibile" — "true" se libero, "false" se occupato (dato derivato
     *                   dalle prenotazioni attive)
     * (tutti i valori come stringhe).
     */
    public static List<List<Map<String, String>>> getMappaOmbrelloni(LocalDate data) {
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        List<List<Map<String, String>>> mappa = new ArrayList<>();

        for (FilaOmbrelloni fila : fileOrdinatePerMappa()) {
            List<Map<String, String>> righeFila = new ArrayList<>();
            for (Ombrellone ombrellone : ombrelloniOrdinati(fila)) {
                righeFila.add(Map.of(
                        "numero", String.valueOf(ombrellone.getNumero()),
                        "id", String.valueOf(ombrellone.getId()),
                        "disponibile", String.valueOf(
                                !registroPrenotazioni.isOmbrelloneOccupato(ombrellone, data))));
            }
            mappa.add(righeFila);
        }

        return mappa;
    }

    /*
     * Prezzo del tipo di fila indicato per la stagione della data scelta
     * (condiviso da tutti gli ombrelloni della fila); -1 se la tariffa non è
     * definita.
     */
    public static double getPrezzoFila(int indiceFila, LocalDate data) {
        List<FilaOmbrelloni> file = fileOrdinatePerMappa();
        if (indiceFila < 0 || indiceFila >= file.size()) {
            return -1;
        }

        TipoFila tipo = file.get(indiceFila).getTipoFila();
        Stagione stagione = stagionePerData(data);

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
    public static String getNomeStagione(LocalDate data) {
        return stagionePerData(data).getEtichetta();
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
    // Visualizzazione Mappa). Il Boundary identifica cliente, ombrellone e
    // servizi con valori semplici (email, id): nessuna Entity attraversa il
    // confine B/C.

    /*
     * Servizi che il cliente può prenotare per la data scelta: quelli con
     * disponibilità residua positiva E con una tariffa definita per la stagione
     * di quella data; gli altri non sono mostrati.
     */
    private static List<ServizioAggiuntivo> serviziSelezionabili(LocalDate data) {
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        Stagione stagione = stagionePerData(data);
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
     * Servizi prenotabili per la data, in un'unica chiamata: una riga per
     * servizio, con chiavi
     *   "descrizione" — descrizione del servizio;
     *   "id"          — id che identifica il servizio scelto nella prenotazione;
     *   "residuo"     — quantità massima ordinabile (dato derivato dalle
     *                   prenotazioni attive);
     *   "prezzo"      — prezzo unitario per la stagione della data
     * (i valori numerici come stringhe). Nessuna Entity verso il Boundary.
     */
    public static List<Map<String, String>> getServiziPrenotabili(LocalDate data) {
        RegistroPrenotazioni registroPrenotazioni = new RegistroPrenotazioni();
        Stagione stagione = stagionePerData(data);
        List<Map<String, String>> righe = new ArrayList<>();

        for (ServizioAggiuntivo servizio : serviziSelezionabili(data)) {
            righe.add(Map.of(
                    "descrizione", servizio.getDescrizione(),
                    "id", String.valueOf(servizio.getId()),
                    "residuo", String.valueOf(registroPrenotazioni.residuoServizio(servizio, data)),
                    "prezzo", String.valueOf(costoServizio(servizio, stagione))));
        }

        return righe;
    }

    /*
     * Prezzo dell'ombrellone (dal tipo della sua fila) per la stagione della
     * data; -1 se la tariffa non è definita o l'ombrellone non esiste. Il
     * Boundary lo ottiene tramite getPrezzoTotale (senza servizi).
     */
    private static double prezzoOmbrellone(long idOmbrellone, LocalDate data) {
        Ombrellone ombrellone = new RegistroOmbrelloni().trovaOmbrellone(idOmbrellone);

        if (ombrellone == null || ombrellone.getFila() == null) {
            return -1;
        }

        return costoTipoFila(ombrellone.getFila().getTipoFila(), stagionePerData(data));
    }

    /*
     * Prezzo totale della prenotazione: ombrellone + servizi per le rispettive
     * quantità (array paralleli), alla stagione della data. Le tariffe non
     * definite (-1) non incidono sul totale.
     */
    public static double getPrezzoTotale(long idOmbrellone, long[] idServiziScelti,
                                      int[] quantita, LocalDate data) {
        double totale = Math.max(0, prezzoOmbrellone(idOmbrellone, data));

        if (idServiziScelti != null && quantita != null
                && idServiziScelti.length == quantita.length) {
            Stagione stagione = stagionePerData(data);
            for (int i = 0; i < idServiziScelti.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio =
                        new RegistroServiziAggiuntivi().trovaServizio(idServiziScelti[i]);
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
     * servizio→quantità (solo quantità positive). Verifica i conflitti
     * interrogando il RegistroPrenotazioni (Information Expert) — disponibilità
     * dell'ombrellone (estensione 2.a) e residuo dei servizi (estensione 3.1.a)
     * — e ne mappa l'esito in un codice per il Boundary, poi delega la creazione
     * al Registro: i conflitti sono query di dominio, non eccezioni di controllo.
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

        Cliente cliente = clientePerEmail(emailCliente);
        Ombrellone ombrellone = new RegistroOmbrelloni().trovaOmbrellone(idOmbrellone);

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
                        new RegistroServiziAggiuntivi().trovaServizio(idServiziScelti[i]);
                if (servizio == null) {
                    return DATI_PRENOTAZIONE_NON_VALIDI;
                }
                quantitaServizi.put(servizio, quantita[i]);
            }
        }

        // Prezzo "congelato" al momento della prenotazione, alle tariffe vigenti.
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

            // La notifica è innescata dal Boundary alla conferma (vedi
            // getMessaggioNotificaPrenotazione): qui si restituisce solo l'esito.
            return PRENOTAZIONE_OK;

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
    // ClienteAutenticato): consultazione dello storico e annullamento entro il
    // limite temporale. Il Boundary identifica il cliente con l'email e la
    // prenotazione con il suo id.

    /*
     * Storico del cliente in un'unica chiamata: una riga per prenotazione, in
     * ordine deterministico (per data, poi per id), con chiavi
     *   "data"        — data prenotata (dd/MM/yyyy);
     *   "postazione"  — postazione scelta ("Ombrellone n. X (Fila Y)");
     *   "servizi"     — servizi aggiuntivi con quantità, o "nessuno";
     *   "stato"       — nome dello stato (Prenotata/Annullata);
     *   "prezzo"      — prezzo totale;
     *   "id"          — id della prenotazione;
     *   "annullabile" — "true"/"false": indica se l'annullamento è possibile
     *                   oggi (stato Prenotata e oggi < data); il Boundary la usa
     *                   per abilitare/disabilitare l'annullamento
     * (i valori non testuali come stringhe). Lista vuota se l'email non
     * corrisponde a un cliente. Nessuna Entity verso il Boundary.
     */
    public static List<Map<String, String>> getPrenotazioniCliente(String emailCliente) {
        Cliente cliente = clientePerEmail(emailCliente);
        if (cliente == null) {
            return List.of();
        }

        List<Prenotazione> prenotazioni = new RegistroPrenotazioni().prenotazioniCliente(cliente);
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
     * Caso d'uso Gestione prenotazioni personali — Annullamento prenotazione.
     *
     * Risolve cliente (per email) e prenotazione (per id), verifica che la
     * prenotazione appartenga a quel cliente e che sia annullabile entro il
     * limite temporale, poi delega al RegistroPrenotazioni la transizione ad
     * Annullata e il salvataggio. Restituisce un codice di esito per il Boundary.
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

            // La notifica è innescata dal Boundary alla conferma (vedi
            // getMessaggioNotificaAnnullamento): qui si restituisce solo l'esito.
            return ANNULLAMENTO_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_ANNULLAMENTO;
        }
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
     * 1 x Cabina"); stringa vuota se non ci sono servizi.
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

    // --- Notifica (testo del messaggio per il Boundary, che innesca la chiamata al canale) ---

    private static final DateTimeFormatter FORMATO_DATA_NOTIFICA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /*
     * Corpo (testo) della notifica di conferma, composto dagli stessi input della
     * prenotazione appena effettuata. Il Boundary lo richiede dopo l'esito OK e
     * lo passa, col destinatario che già conosce, al proprio Adapter; null se i
     * dati non sono risolvibili (il Boundary salta la notifica). Attraversa il
     * confine solo una String: nessuna Entity né oggetto di trasferimento.
     */
    public static String getMessaggioNotificaPrenotazione(String emailCliente, long idOmbrellone,
                                                        LocalDate data, long[] idServizi, int[] quantita) {
        Cliente cliente = clientePerEmail(emailCliente);
        Ombrellone ombrellone = new RegistroOmbrelloni().trovaOmbrellone(idOmbrellone);
        if (cliente == null || ombrellone == null) {
            return null;
        }

        // Mappa servizio -> quantità (solo quantità positive; i servizi non
        // risolvibili sono semplicemente ignorati nel messaggio).
        Map<ServizioAggiuntivo, Integer> quantitaServizi = new LinkedHashMap<>();
        if (idServizi != null && quantita != null && idServizi.length == quantita.length) {
            for (int i = 0; i < idServizi.length; i++) {
                if (quantita[i] <= 0) {
                    continue;
                }
                ServizioAggiuntivo servizio = new RegistroServiziAggiuntivi().trovaServizio(idServizi[i]);
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

    /*
     * Corpo (testo) della notifica di annullamento per la prenotazione indicata,
     * se appartiene al cliente; null se non risolvibile o non di proprietà.
     */
    public static String getMessaggioNotificaAnnullamento(String emailCliente, long idPrenotazione) {
        Cliente cliente = clientePerEmail(emailCliente);
        if (cliente == null) {
            return null;
        }
        Prenotazione prenotazione = new RegistroPrenotazioni().trovaPrenotazione(idPrenotazione);
        if (prenotazione == null || !appartieneA(prenotazione, cliente)) {
            return null;
        }
        return componiCorpoAnnullamento(prenotazione);
    }

    /*
     * Testo della conferma: saluto, ombrellone, data, servizi inclusi e totale.
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
     * Testo dell'annullamento: saluto, ombrellone e data.
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
     * Riga di saluto iniziale ("Gentile <nome cognome>,") comune ai due messaggi.
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

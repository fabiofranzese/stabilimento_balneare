package controller;

import entity.FilaOmbrelloni;
import entity.Ombrellone;
import entity.RegistroOmbrelloni;
import entity.RegistroPrenotazioni;
import entity.RegistroServiziAggiuntivi;
import entity.RegistroTariffe;
import entity.ServizioAggiuntivo;
import entity.Stagione;
import entity.TariffaServizioAggiuntivo;
import entity.TariffaTipoFila;
import entity.TipoFila;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * GestoreStabilimento è il Controller e la Façade dei casi d'uso che
 * ruotano attorno allo stabilimento: Configurazione stabilimento, Definizione
 * tariffe e Visualizzazione mappa.
 */
public class GestoreStabilimento {

    // Esiti della configurazione.
    public static final int CONFIGURAZIONE_OK = 0;
    public static final int DATI_NON_VALIDI = 1;
    public static final int ERRORE_CONFIGURAZIONE = 2;
    public static final int PRENOTAZIONI_PRESENTI = 3;

    // Esiti della definizione tariffe.
    public static final int TARIFFE_OK = 10;
    public static final int TARIFFA_NON_VALIDA = 11;
    public static final int ERRORE_TARIFFE = 12;

    /*
     * CONFIGURAZIONE STABILIMENTO
     *
     * 1. Riceve la disposizione: numero di ombrelloni per fila, descrizioni dei servizi
     * aggiuntivi e loro capacità (array paralleli per i servizi).
     * 2. Valida i dati, e rigenera disposizione e servizi.
     * La posizione delle file non è scelta dal gestore, ma è derivata dall'ordine
     * (tipoFilaPerPosizione) e passata al RegistroOmbrelloni.
     */
    public static int salvaConfigurazione(int[] ombrelloniPerFila,
                                          String[] descrizioniServizi, int[] capacitaServizi) {

        if (!isDatiValidi(ombrelloniPerFila, descrizioniServizi, capacitaServizi)) {
            return DATI_NON_VALIDI;
        }

        // Precondizione: Dato che la riconfigurazione è distruttiva (sostituisce file e
        // servizi), è bloccata se esiste anche una sola prenotazione attiva.
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
     * Lettura delle file correnti, per precaricare il form.
     * Una riga per fila, nell'ordine della disposizione, con chiave
     * "numeroOmbrelloni" e valore numero di ombrelloni della fila.
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
     * Lettura dei servizi aggiuntivi correnti, per precaricare il form.
     * Una riga per servizio, con chiavi:
     * "descrizione" — descrizione del servizio;
     * "capacita"— capacità giornaliera.
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
     * Etichette in italiano della posizione di ogni fila, derivate dall'ordine.
     * Il Boundary le usa per mostrare la posizione assegnata dal sistema, la quale
     * viene aggiornata a ogni aggiunta/rimozione di una fila.
     */
    public static String[] getEtichettePosizioniFile(int numeroFile) {
        String[] etichette = new String[Math.max(0, numeroFile)];

        for (int i = 0; i < etichette.length; i++) {
            etichette[i] = etichettaEnum(tipoFilaPerPosizione(i, numeroFile));
        }

        return etichette;
    }

    /*
     * Regola di derivazione della posizione: la posizione di una fila dipende
     * dal suo ordine nello stabilimento, non è scelta dal gestore. La prima è
     * PRIMA_FILA, l'ultima è ULTIMA_FILA, quelle in mezzo FILA_INTERMEDIA.
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
     * Etichetta leggibile ricavata dal nome della costante enum:
     * sostituisce gli underscore con spazi e rende maiuscola l'iniziale
     * (es. PRIMA_FILA -> "Prima fila", ALTA -> "Alta").
     */
    private static String etichettaEnum(Enum<?> valore) {
        String testo = valore.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(testo.charAt(0)) + testo.substring(1);
    }

    /*
     * Validazione della configurazione:
     * almeno una fila, ciascuna con almeno un ombrellone, array dei
     * servizi coerenti in lunghezza; descrizioni non vuote; 
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

    /*
     * DEFINIZIONE TARIFFE
     *
     * Le tariffe definite sono in 3 array paralleli elemento + stagione + costo),
     * con ogni tariffa indicata viene impostata o aggiornata e quelle non più
     * presenti che vengono eliminate dal database.
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

            // Chiavi (elemento + stagione) inviate dal gestore.
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
     * e indice della stagione.
     */
    private static String chiaveTariffa(int elemento, int stagioneindice) {
        return elemento + ":" + stagioneindice;
    }

    /*
     * Validazione delle tariffe: array coerenti in lunghezza, indici validi,
     * costi strettamente positivi.
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
     * Etichette delle stagioni, nell'ordine dell'enum Stagione: l'etichetta è
     * derivata dal nome della costante con il suffisso " stagione".
     * Questi popolano la combo del form.
     */
    public static String[] getStagioni() {
        Stagione[] valori = Stagione.values();
        String[] etichette = new String[valori.length];

        for (int i = 0; i < valori.length; i++) {
            etichette[i] = etichettaEnum(valori[i]) + " stagione";
        }

        return etichette;
    }

    /*
     * Stagione in cui cade una data:
     * Da giugno a settembre è alta stagione, altrimenti bassa.
     */
    private static Stagione stagionePerData(LocalDate data) {
        int mese = data.getMonthValue();
        return (mese >= 6 && mese <= 9) ? Stagione.ALTA : Stagione.BASSA;
    }

    /*
     * Indica se lo stabilimento è già stato configurato (esiste almeno una fila),
     * dato che la configurazione è precondizione della definizione delle tariffe
     */
    public static boolean isConfigurazioneEffettuata() {
        return !new RegistroOmbrelloni().getFile().isEmpty();
    }

    /*
     * Etichette dei possibili elementi di una tariffa, per la combo del form:
     * i tipi di fila effettivamente configurati e i servizi aggiuntivi.
     * L'indice scelto identifica l'elemento: dat 0 a numTipi-1 i tipi di fila, da
     * numTipi in poi i servizi.
     */
    public static String[] getElementiTariffa() {
        List<TipoFila> tipi = tipiFilaConfigurati();
        List<ServizioAggiuntivo> servizi = serviziOrdinati();

        String[] etichette = new String[tipi.size() + servizi.size()];

        for (int i = 0; i < tipi.size(); i++) {
            etichette[i] = "Ombrellone — " + etichettaEnum(tipi.get(i));
        }
        for (int i = 0; i < servizi.size(); i++) {
            etichette[tipi.size() + i] = "Servizio — " + servizi.get(i).getDescrizione();
        }

        return etichette;
    }

    /*
     * Lettura delle tariffe correnti per pre-caricare il form in un'unica
     * chiamata. Ritorna una riga per tariffa, con chiavi:
     * "elemento": indice dell'elemento (vedi getElementiTariffa);
     * "stagione": indice della stagione (vedi getStagioni);
     * "costo": costo della tariffa
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
     * Tipi di fila configurati da ottenere dato che si possono tariffare
     *  solo i tipi di fila che esistono nello stabilimento.
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
     * Servizi aggiuntivi configurati da ottenere dato che si possono tariffare
     * solo i servizi che esistono nello stabilimento
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

    /*
     * VISUALIZZAZIONE MAPPA
     * Il Boundary riceve solo etichette e righe di stringhe a chiavi, allineate
     * per indice di fila e poi di ombrellone. Le file sono ordinate per
     * posizione (prima -> intermedia -> ultima) e poi per numero.
     * La mappa che va dalla riva verso l'interno.
     */

    /*
     * Mappa degli ombrelloni per la data scelta, in un'unica chiamata: una lista
     * per fila (stesso ordine di getEtichetteFile), con una riga per ombrellone
     * dalle chiavi
     *   "numero": numero dell'ombrellone;
     *   "id": identifica l'ombrellone selezionato sulla mappa per avviarne la prenotazione;
     *   "disponibile": true se libero, false se occupato (derivato dalle prenotazioni attive)
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
     * Etichette delle file: "Fila N".
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
     * Prezzo del tipo di fila indicato per la stagione della data scelta
     * Il prezzo è -1 se la tariffa non è definita.
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
     * Nome della stagione in cui cade la data scelta, derivato dal nome della
     * costante enum con il suffisso " stagione".
     */
    public static String getNomeStagione(LocalDate data) {
        return etichettaEnum(stagionePerData(data)) + " stagione";
    }

    /*
     * File e ombrelloni ordinati per la mappa: prima per posizione (prima -> intermedia -> ultima) e poi per numero
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

    /*
     * EFFETTUA PRENOTAZIONE (per GestorePrenotazioni)
     */

    /*
     * Servizi prenotabili per la data: una riga per servizio, con chiavi
     *   "descrizione": descrizione del servizio;
     *   "id": identifica il servizio scelto nella prenotazione;
     *   "residuo": quantità massima ordinabile (derivato dalle prenotazioni attive);
     *   "prezzo": prezzo per la stagione della data
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
     * Servizi che il cliente può prenotare per la data scelta: quelli con
     * disponibilità residua positiva e con una tariffa definita per la stagione di quella data.
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
     * Prezzo totale della prenotazione: ombrellone + servizi per le rispettive
     * quantità (array paralleli), alla stagione della data.
     * Le tariffe non definite (-1) non incidono sul totale.
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
     * Prezzo dell'ombrellone dal tipo della sua fila per la stagione della data
     * Restituisce -1 se la tariffa non è definita o l'ombrellone non esiste.
     */
    private static double prezzoOmbrellone(long idOmbrellone, LocalDate data) {
        Ombrellone ombrellone = new RegistroOmbrelloni().trovaOmbrellone(idOmbrellone);

        if (ombrellone == null || ombrellone.getFila() == null) {
            return -1;
        }

        return costoTipoFila(ombrellone.getFila().getTipoFila(), stagionePerData(data));
    }

    /*
     * Ombrellone e Servizio Aggiuntivo per id. Esposti a GestorePrenotazioni, che compone la
     * prenotazione senza accedere direttamente al RegistroOmbrelloni e al RegistroServiziAggiuntivi.
     */
    public static Ombrellone trovaOmbrellone(long id) {
        return new RegistroOmbrelloni().trovaOmbrellone(id);
    }

    public static ServizioAggiuntivo trovaServizio(long id) {
        return new RegistroServiziAggiuntivi().trovaServizio(id);
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

}

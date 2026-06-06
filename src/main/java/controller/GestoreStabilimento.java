package controller;

import entity.FilaOmbrelloni;
import entity.RegistroOmbrelloni;
import entity.RegistroServiziAggiuntivi;
import entity.ServizioAggiuntivo;
import entity.TipoFila;

import java.util.List;

/*
 * GestoreStabilimento è il Controller (GRASP) e la Façade dei casi d'uso del
 * Gestore. Per ora copre la Configurazione stabilimento; le operazioni di
 * prenotazione/monitoraggio previste dal modello verranno aggiunte qui nei casi
 * d'uso successivi.
 *
 * Come GestoreUtenti, espone operazioni a grana grossa e scambia con il Boundary
 * solo tipi primitivi (int, String[], int[]): il Boundary non conosce le Entity
 * (FilaOmbrelloni, Ombrellone, ServizioAggiuntivo, TipoFila) e non importa il
 * package entity, rispettando la separazione BCED.
 *
 * NOTE: il modello prevede metodi a grana fine (aggiungiFila(tipoFila),
 * aggiungiOmbrellone(fila, numero)); esporli al Boundary farebbe però transitare
 * un'Entity (la fila) verso la GUI, violando BCED. Si usa quindi un'unica
 * operazione salvaConfigurazione: la Façade compone internamente le file e i
 * loro ombrelloni tramite FilaOmbrelloni.creaOmbrellone.
 */
public class GestoreStabilimento {

    // Esiti della configurazione.
    public static final int CONFIGURAZIONE_OK = 0;
    public static final int DATI_NON_VALIDI = 1;
    public static final int ERRORE_CONFIGURAZIONE = 2;

    /*
     * Etichette in italiano dei tipi di fila, nell'ordine dell'enum TipoFila:
     * servono a popolare la combo del form. L'indice scelto nella combo
     * corrisponde a TipoFila.values()[indice].
     */
    public static String[] tipiFila() {
        TipoFila[] valori = TipoFila.values();
        String[] etichette = new String[valori.length];

        for (int i = 0; i < valori.length; i++) {
            etichette[i] = valori[i].getEtichetta();
        }

        return etichette;
    }

    /*
     * Caso d'uso Configurazione stabilimento.
     *
     * Riceve la disposizione (per ogni fila: indice del tipo + numero di
     * ombrelloni) e i servizi aggiuntivi (per ognuno: descrizione + capacità).
     * Valida i dati, poi rigenera disposizione e servizi (strategia "replace").
     *
     * Gli array delle file (tipiFilaIndici, ombrelloniPerFila) sono paralleli,
     * come quelli dei servizi (descrizioniServizi, capacitaServizi).
     */
    public static int salvaConfigurazione(int[] tipiFilaIndici, int[] ombrelloniPerFila,
                                          String[] descrizioniServizi, int[] capacitaServizi) {

        if (!datiValidi(tipiFilaIndici, ombrelloniPerFila, descrizioniServizi, capacitaServizi)) {
            return DATI_NON_VALIDI;
        }

        try {
            TipoFila[] valori = TipoFila.values();
            TipoFila[] tipi = new TipoFila[tipiFilaIndici.length];
            for (int i = 0; i < tipiFilaIndici.length; i++) {
                tipi[i] = valori[tipiFilaIndici[i]];
            }

            new RegistroOmbrelloni().configuraDisposizione(tipi, ombrelloniPerFila);
            new RegistroServiziAggiuntivi().sostituisciServizi(descrizioniServizi, capacitaServizi);

            return CONFIGURAZIONE_OK;

        } catch (RuntimeException e) {
            e.printStackTrace();
            return ERRORE_CONFIGURAZIONE;
        }
    }

    // --- Lettura della configurazione corrente (per precaricare il form) ---
    // Si restituiscono solo tipi primitivi/array: nessuna Entity verso il Boundary.

    public static int[] tipiFilaCorrenti() {
        List<FilaOmbrelloni> file = new RegistroOmbrelloni().getFile();
        int[] indici = new int[file.size()];

        for (int i = 0; i < file.size(); i++) {
            indici[i] = file.get(i).getTipoFila().ordinal();
        }

        return indici;
    }

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
     * - almeno una fila;
     * - array delle file coerenti in lunghezza; ogni indice di tipo valido;
     *   ogni fila con almeno un ombrellone;
     * - array dei servizi coerenti in lunghezza; ogni descrizione non vuota;
     *   ogni capacità non negativa.
     */
    private static boolean datiValidi(int[] tipiFilaIndici, int[] ombrelloniPerFila,
                                      String[] descrizioniServizi, int[] capacitaServizi) {

        if (tipiFilaIndici == null || ombrelloniPerFila == null
                || descrizioniServizi == null || capacitaServizi == null) {
            return false;
        }

        if (tipiFilaIndici.length == 0 || tipiFilaIndici.length != ombrelloniPerFila.length) {
            return false;
        }

        int numeroTipi = TipoFila.values().length;
        for (int i = 0; i < tipiFilaIndici.length; i++) {
            if (tipiFilaIndici[i] < 0 || tipiFilaIndici[i] >= numeroTipi) {
                return false;
            }
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
}

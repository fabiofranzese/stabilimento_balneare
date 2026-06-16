package entity;

import database.GestorePersistenza;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
 * RegistroOmbrelloni è il servizio per la disposizione degli ombrelloni.
 *
 */
public class RegistroOmbrelloni {

    private GestorePersistenza gestorePersistenza;

    public RegistroOmbrelloni() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * CONFIGURAZIONE STABILIMENTO
     *
     * Definisce la disposizione con la seguente strategia:
     * La configurazione precedente viene rimossa e rigenerata.
     */
    public void configuraDisposizione(TipoFila[] tipiFile, int[] ombrelloniPerFila) {
        eliminaTutteLeFile();

        int totale = ombrelloniPerFila.length;
        for (int i = 0; i < totale; i++) {
            FilaOmbrelloni fila = new FilaOmbrelloni(i + 1, tipiFile[i]);

            for (int numero = 1; numero <= ombrelloniPerFila[i]; numero++) {
                fila.creaOmbrellone(numero);
            }

            gestorePersistenza.salva(fila);
        }
    }

    /*
     * Cerca un ombrellone per id (null se inesistente).
     */
    public Ombrellone trovaOmbrellone(long id) {
        return gestorePersistenza.trovaPerId(Ombrellone.class, id);
    }

    /*
     * Restituisce tutte le file, ordinate per numero.
     */
    public List<FilaOmbrelloni> getFile() {
        List<FilaOmbrelloni> file = gestorePersistenza.cercaPerCampi(FilaOmbrelloni.class, Map.of());
        file.sort(Comparator.comparingInt(FilaOmbrelloni::getNumero));
        return file;
    }

    /*
     * Rimuove tutte le file esistenti, con l'eliminazione che propaga ai relativi ombrelloni.
     */
    private void eliminaTutteLeFile() {
        for (FilaOmbrelloni fila : getFile()) {
            gestorePersistenza.elimina(FilaOmbrelloni.class, fila.getId());
        }
    }
}

package entity;

import database.GestorePersistenza;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
 * RegistroOmbrelloni è il servizio di dominio per la disposizione degli
 * ombrelloni (livello Entity, BCED).
 *
 * Ruoli GRASP:
 * - Creator: è responsabile della creazione delle FilaOmbrelloni (la creazione
 *   dei singoli Ombrellone è delegata alla fila stessa, che li aggrega);
 * - Information Expert: conosce l'insieme delle file, quindi gli compete la loro
 *   ricerca complessiva (getFile).
 *
 * Come gli altri Registro, usa GestorePersistenza (livello Database): la logica
 * di dominio resta qui, il codice tecnico di persistenza resta nel package
 * database (dipendenza Entity -> Database accettata come idioma del progetto).
 */
public class RegistroOmbrelloni {

    private GestorePersistenza gestorePersistenza;

    public RegistroOmbrelloni() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Caso d'uso Configurazione stabilimento: (ri)definisce la disposizione.
     *
     * Strategia "replace": la configurazione precedente viene rimossa e
     * rigenerata a partire dai dati indicati. Per ogni fila si crea la
     * FilaOmbrelloni con la sua posizione, poi i suoi ombrelloni numerati 1..N;
     * il salvataggio della fila propaga (cascade) agli ombrelloni.
     *
     * I due array sono paralleli: tipi[i] e ombrelloniPerFila[i] descrivono la
     * fila i-esima (numerata i+1).
     */
    public void configuraDisposizione(TipoFila[] tipi, int[] ombrelloniPerFila) {
        eliminaTutteLeFile();

        for (int i = 0; i < tipi.length; i++) {
            FilaOmbrelloni fila = new FilaOmbrelloni(i + 1, tipi[i]);

            for (int numero = 1; numero <= ombrelloniPerFila[i]; numero++) {
                fila.creaOmbrellone(numero);
            }

            gestorePersistenza.salva(fila);
        }
    }

    /*
     * Information Expert: restituisce tutte le file, ordinate per numero.
     */
    public List<FilaOmbrelloni> getFile() {
        List<FilaOmbrelloni> file = gestorePersistenza.cercaPerCampi(FilaOmbrelloni.class, Map.of());
        file.sort(Comparator.comparingInt(FilaOmbrelloni::getNumero));
        return file;
    }

    /*
     * Rimuove tutte le file esistenti; l'eliminazione propaga (cascade +
     * orphanRemoval) ai relativi ombrelloni.
     */
    private void eliminaTutteLeFile() {
        for (FilaOmbrelloni fila : getFile()) {
            gestorePersistenza.elimina(FilaOmbrelloni.class, fila.getId());
        }
    }
}

package entity;

import database.GestorePersistenza;

import java.util.List;
import java.util.Map;

/*
 * RegistroTariffe è il servizio di dominio per le tariffe (livello Entity, BCED).
 *
 * Ruoli GRASP:
 * - Creator: crea le istanze di Tariffa (TariffaTipoFila / TariffaServizioAggiuntivo);
 * - Information Expert: conosce l'insieme delle tariffe, quindi gli competono le
 *   ricerche complessive (getTariffeTipoFila / getTariffeServizio).
 *
 * Usa GestorePersistenza (livello Database), come gli altri Registro.
 *
 * Strategia "upsert": definire una tariffa per un certo elemento e una certa
 * stagione aggiorna il costo se la tariffa esiste già, altrimenti la crea. In
 * questo modo esiste al più una tariffa per ogni coppia (elemento, stagione).
 */
public class RegistroTariffe {

    private GestorePersistenza gestorePersistenza;

    public RegistroTariffe() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Definisce (crea o aggiorna) la tariffa di un tipo di fila per una stagione.
     */
    public void definisciTariffaTipoFila(TipoFila tipoFila, Stagione stagione, double costo) {
        TariffaTipoFila esistente = gestorePersistenza.cercaPrimoPerCampi(
                TariffaTipoFila.class,
                Map.of("tipoFila", tipoFila, "stagione", stagione));

        if (esistente != null) {
            esistente.setCosto(costo);
            gestorePersistenza.aggiorna(esistente);
        } else {
            gestorePersistenza.salva(new TariffaTipoFila(tipoFila, costo, stagione));
        }
    }

    /*
     * Definisce (crea o aggiorna) la tariffa di un servizio aggiuntivo per una
     * stagione.
     */
    public void definisciTariffaServizio(ServizioAggiuntivo servizio, Stagione stagione, double costo) {
        TariffaServizioAggiuntivo esistente = gestorePersistenza.cercaPrimoPerCampi(
                TariffaServizioAggiuntivo.class,
                Map.of("servizio", servizio, "stagione", stagione));

        if (esistente != null) {
            esistente.setCosto(costo);
            gestorePersistenza.aggiorna(esistente);
        } else {
            gestorePersistenza.salva(new TariffaServizioAggiuntivo(servizio, costo, stagione));
        }
    }

    /*
     * Elimina la tariffa indicata (per id). Usata dalla riconciliazione del
     * salvataggio per cancellare le tariffe rimosse dal gestore.
     */
    public void elimina(Tariffa tariffa) {
        if (tariffa != null && tariffa.getId() != null) {
            // Tariffa.class basta anche per le sottoclassi (mappatura SINGLE_TABLE).
            gestorePersistenza.elimina(Tariffa.class, tariffa.getId());
        }
    }

    /*
     * Information Expert: tutte le tariffe dei tipi di fila.
     */
    public List<TariffaTipoFila> getTariffeTipoFila() {
        return gestorePersistenza.cercaPerCampi(TariffaTipoFila.class, Map.of());
    }

    /*
     * Information Expert: tutte le tariffe dei servizi aggiuntivi.
     */
    public List<TariffaServizioAggiuntivo> getTariffeServizio() {
        return gestorePersistenza.cercaPerCampi(TariffaServizioAggiuntivo.class, Map.of());
    }
}

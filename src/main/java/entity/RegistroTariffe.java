package entity;

import database.GestorePersistenza;

import java.util.List;
import java.util.Map;

/*
 * RegistroTariffe è il servizio di dominio per le tariffe.
 */
public class RegistroTariffe {

    private GestorePersistenza gestorePersistenza;

    public RegistroTariffe() {
        gestorePersistenza = new GestorePersistenza();
    }


    /*
     * DEFINIZIONE TARIFFE
     *
     * Definisce la tariffa di un tipo di fila per una stagione.
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
     * Definisce la tariffa di un servizio aggiuntivo per una stagione.
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
     * Elimina la tariffa indicata (per id).
     */
    public void elimina(Tariffa tariffa) {
        if (tariffa != null && tariffa.getId() != null) {
            // Tariffa.class basta anche per le sottoclassi (mappatura SINGLE_TABLE).
            gestorePersistenza.elimina(Tariffa.class, tariffa.getId());
        }
    }

    public List<TariffaTipoFila> getTariffeTipoFila() {
        return gestorePersistenza.cercaPerCampi(TariffaTipoFila.class, Map.of());
    }

    public List<TariffaServizioAggiuntivo> getTariffeServizio() {
        return gestorePersistenza.cercaPerCampi(TariffaServizioAggiuntivo.class, Map.of());
    }
}

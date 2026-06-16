package entity;

import jakarta.persistence.*;

/*
 * TariffaTipoFila è il prezzo di un ombrellone in base alla posizione della sua fila, per una certa stagione.
 * È sottoclasse concreta di Tariffa con discriminatore "TIPO_FILA".
 */
@Entity
@DiscriminatorValue("TIPO_FILA")
public class TariffaTipoFila extends Tariffa {

    @Enumerated(EnumType.STRING)
    private TipoFila tipoFila;

    public TariffaTipoFila() {
    }

    public TariffaTipoFila(TipoFila tipoFila, double costo, Stagione stagione) {
        super(costo, stagione);
        this.tipoFila = tipoFila;
    }

    public TipoFila getTipoFila() {
        return tipoFila;
    }

    public void setTipoFila(TipoFila tipoFila) {
        this.tipoFila = tipoFila;
    }
}

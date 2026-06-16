package entity;

/*
 * TipoFila rappresenta la posizione di una fila di ombrelloni all'interno dello
 * stabilimento.
 */
public enum TipoFila {

    PRIMA_FILA("Prima fila"),
    FILA_INTERMEDIA("Fila intermedia"),
    ULTIMA_FILA("Ultima fila");

    private final String etichetta;

    TipoFila(String etichetta) {
        this.etichetta = etichetta;
    }

    public String getEtichetta() {
        return etichetta;
    }
}

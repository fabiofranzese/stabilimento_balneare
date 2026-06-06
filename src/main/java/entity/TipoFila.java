package entity;

/*
 * TipoFila rappresenta la posizione di una fila di ombrelloni all'interno dello
 * stabilimento (livello Entity, BCED).
 *
 * Il domain model la disegna come gerarchia astratta (TipoFila → PrimaFila /
 * FilaIntermedia / UltimaFila); poiché è un insieme chiuso di tre valori senza
 * comportamento, qui è realizzata come enum: più semplice da mappare con JPA
 * (@Enumerated su FilaOmbrelloni) e da usare. L'etichetta in italiano è l'unica
 * sorgente dei testi mostrati nella GUI (vedi GestoreStabilimento.tipiFila()).
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

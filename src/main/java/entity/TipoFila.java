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

    /*
     * Regola di dominio: la posizione di una fila è derivata dal suo ordine nello
     * stabilimento, non scelta dal gestore. La prima fila è la PRIMA_FILA (anche
     * se è l'unica), l'ultima è la ULTIMA_FILA, tutte quelle in mezzo sono
     * FILA_INTERMEDIA. Il primo controllo che riesce vince: così con una sola fila
     * il risultato è PRIMA_FILA.
     */
    public static TipoFila perPosizione(int indice, int totale) {
        if (indice <= 0) {
            return PRIMA_FILA;
        }
        if (indice >= totale - 1) {
            return ULTIMA_FILA;
        }
        return FILA_INTERMEDIA;
    }
}

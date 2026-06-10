package entity;

/*
 * TipoFila rappresenta la posizione di una fila di ombrelloni all'interno dello
 * stabilimento (livello Entity, BCED).
 *
 * Il domain model la disegna come gerarchia astratta (TipoFila → PrimaFila /
 * FilaIntermedia / UltimaFila); essendo un insieme chiuso di tre valori senza
 * comportamento, è realizzata come enum, più semplice da mappare con JPA
 * (@Enumerated su FilaOmbrelloni). L'etichetta in italiano è l'unica sorgente
 * dei testi mostrati nella GUI (vedi GestoreStabilimento.etichettePosizioniFile
 * ed elementiTariffa).
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
     * stabilimento, non scelta dal gestore. La prima è PRIMA_FILA (anche se è
     * l'unica: il primo controllo vince), l'ultima è ULTIMA_FILA, quelle in mezzo
     * FILA_INTERMEDIA.
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

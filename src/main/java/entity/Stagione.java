package entity;

/*
 * Stagione rappresenta il periodo di applicazione di una tariffa (livello
 * Entity, BCED): alta o bassa stagione.
 *
 * Il domain model la disegna come gerarchia astratta (Stagione → Alta / Bassa);
 * essendo un insieme chiuso di valori senza comportamento, qui è realizzata come
 * enum, coerentemente con TipoFila. L'etichetta in italiano è l'unica sorgente
 * dei testi mostrati nella GUI (vedi GestoreStabilimento.stagioni()).
 *
 * NOTE: la corrispondenza fra una data del calendario e la sua stagione (il
 * "periodo" come intervallo di date) non serve per definire le tariffe: verrà
 * affrontata nel caso d'uso di prenotazione, quando si calcolerà il prezzo per
 * una data specifica.
 */
public enum Stagione {

    ALTA("Alta stagione"),
    BASSA("Bassa stagione");

    private final String etichetta;

    Stagione(String etichetta) {
        this.etichetta = etichetta;
    }

    public String getEtichetta() {
        return etichetta;
    }
}

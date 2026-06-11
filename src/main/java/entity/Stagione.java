package entity;

/*
 * Stagione rappresenta il periodo di applicazione di una tariffa (livello
 * Entity, BCED): alta o bassa stagione.
 *
 * Il domain model la disegna come gerarchia astratta (Stagione → Alta / Bassa);
 * essendo un insieme chiuso di valori senza comportamento, è realizzata come
 * enum, coerentemente con TipoFila. L'etichetta in italiano è l'unica sorgente
 * dei testi mostrati nella GUI (vedi GestoreStabilimento.getStagioni()). La
 * stagione in cui cade una data è derivata in GestoreStabilimento
 * (stagionePerData).
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

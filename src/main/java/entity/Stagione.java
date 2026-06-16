package entity;

/*
 * Stagione rappresenta il periodo di applicazione di una tariffa: alta o bassa stagione.
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

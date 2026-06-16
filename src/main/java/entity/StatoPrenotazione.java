package entity;

/*
 * StatoPrenotazione permette di applicare il pattern State a Prenotazione.
 * Le sottoclassi concrete Prenotata e Annullata incapsulano il comportamento che varia con lo stato.
 */
public abstract class StatoPrenotazione {

    /*
     * Una prenotazione è "attiva" se occupa effettivamente la postazione.
     */
    public abstract boolean isAttiva();

    /*
     * Indica se da questo stato è ammessa la transizione verso "annullata".
     */
    public abstract boolean isAnnullabile();

    /*
     * Ogni stato concreto decide la transizione:
     * Prenotata passa ad Annullata (setStato(new Annullata()));
     * Annullata non ha alcuna transizione prevista.
     */
    public abstract void annulla(Prenotazione prenotazione);

    /*
     * Nome leggibile dello stato (per la GUI).
     */
    public abstract String nome();

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

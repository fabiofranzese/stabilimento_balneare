package entity;

/*
 * Stato concreto di una prenotazione annullata.
 */
public class Annullata implements StatoPrenotazione {

    /*
     * Una prenotazione già annullata ignora l'evento: nessuna transizione prevista.
     */
    @Override
    public void annulla(Prenotazione prenotazione) {
        // nessuna transizione prevista.
    }
}

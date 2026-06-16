package entity;

/*
 * Stato concreto di una prenotazione annullata.
 */
public class Annullata extends StatoPrenotazione {

    @Override
    public boolean isAttiva() {
        return false;
    }

    @Override
    public boolean isAnnullabile() {
        return false;
    }

    @Override
    public void annulla(Prenotazione prenotazione) {
        // nessuna transizione prevista.
    }

    @Override
    public String nome() {
        return "Annullata";
    }
}

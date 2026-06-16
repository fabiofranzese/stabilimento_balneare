package entity;

/*
 * Stato concreto di una prenotazione attiva.
 */
public class Prenotata extends StatoPrenotazione {

    @Override
    public boolean isAttiva() {
        return true;
    }

    @Override
    public boolean isAnnullabile() {
        return true;
    }

    @Override
    public void annulla(Prenotazione prenotazione) {
        prenotazione.setStato(new Annullata());
    }

    @Override
    public String nome() {
        return "Prenotata";
    }
}

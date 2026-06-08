package entity;

/*
 * Prenotata: stato concreto di una prenotazione attiva. Una prenotazione in
 * questo stato occupa la postazione per la sua data. Oggetto comportamentale
 * (non Entity): persistito come "PRENOTATA" nella colonna tipo_stato di
 * Prenotazione.
 */
public class Prenotata extends StatoPrenotazione {

    @Override
    public boolean isAttiva() {
        return true;
    }

    @Override
    public boolean isAnnullabile() {
        // Una prenotazione attiva può essere annullata (nel rispetto del limite
        // temporale, verificato da Prenotazione.isAnnullabile).
        return true;
    }

    @Override
    public void annulla(Prenotazione prenotazione) {
        // Transizione di stato (pattern State): Prenotata -> Annullata.
        prenotazione.setStato(new Annullata());
    }

    @Override
    public String nome() {
        return "Prenotata";
    }
}

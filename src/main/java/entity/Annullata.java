package entity;

/*
 * Annullata: stato concreto di una prenotazione annullata. Non occupa più la
 * postazione, che torna quindi disponibile per la sua data. Oggetto
 * comportamentale (non Entity): persistito come "ANNULLATA" nella colonna
 * tipo_stato di Prenotazione.
 */
public class Annullata extends StatoPrenotazione {

    @Override
    public boolean isAttiva() {
        return false;
    }

    @Override
    public boolean isAnnullabile() {
        // Una prenotazione già annullata non può essere annullata di nuovo.
        return false;
    }

    @Override
    public void annulla(Prenotazione prenotazione) {
        // nessuna transizione prevista (come nell'esempio StatoPorta del docente):
        // annullare una prenotazione già Annullata non ha effetto.
    }

    @Override
    public String nome() {
        return "Annullata";
    }
}

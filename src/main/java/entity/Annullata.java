package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/*
 * Annullata: stato concreto di una prenotazione annullata. Non occupa più la
 * postazione, che torna quindi disponibile per la sua data.
 */
@Entity
@DiscriminatorValue("ANNULLATA")
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
    public String nome() {
        return "Annullata";
    }
}

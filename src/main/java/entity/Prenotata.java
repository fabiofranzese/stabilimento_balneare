package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/*
 * Prenotata: stato concreto di una prenotazione attiva. Una prenotazione in
 * questo stato occupa la postazione per la sua data.
 */
@Entity
@DiscriminatorValue("PRENOTATA")
public class Prenotata extends StatoPrenotazione {

    @Override
    public boolean isAttiva() {
        return true;
    }

    @Override
    public String nome() {
        return "Prenotata";
    }
}

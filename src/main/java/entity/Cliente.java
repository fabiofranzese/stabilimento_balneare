package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/*
 * Cliente è un Utente con ruolo "CLIENTE".
 *
 * La relazione con Prenotazione ("ha effettuato" 0..*) verrà aggiunta quando
 * implementeremo i casi d'uso relativi alle prenotazioni.
 */
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Utente {

    public Cliente() {
    }

    public Cliente(String nome, String cognome, String email, String telefono, String password) {
        super(nome, cognome, email, telefono, password);
    }
}

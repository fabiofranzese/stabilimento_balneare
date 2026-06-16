package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/*
 * Gestore è un Utente con ruolo "GESTORE".
 *
 * Gli account Gestore non si creano dalla registrazione (riservata ai Cliente), ma
 * vengono predisposti tramite il livello setup (vedi DatiIniziali).
 */
@Entity
@DiscriminatorValue("GESTORE")
public class Gestore extends Utente {

    public Gestore() {
    }

    public Gestore(String nome, String cognome, String email, String telefono, String password) {
        super(nome, cognome, email, telefono, password);
    }
}

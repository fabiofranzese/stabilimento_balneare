package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

/*
 * Cliente è un Utente con ruolo "CLIENTE".
 *
 * Partecipa all'associazione "ha effettuato" (1 — 0..*) con Prenotazione.
 */
@Entity
@DiscriminatorValue("CLIENTE")
public class Cliente extends Utente {

    @OneToMany(mappedBy = "cliente")
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    public Cliente() {
    }

    public Cliente(String nome, String cognome, String email, String telefono, String password) {
        super(nome, cognome, email, telefono, password);
    }

    public List<Prenotazione> getPrenotazioni() {
        return prenotazioni;
    }

    public void setPrenotazioni(List<Prenotazione> prenotazioni) {
        this.prenotazioni = prenotazioni;
    }
}

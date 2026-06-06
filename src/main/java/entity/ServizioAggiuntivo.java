package entity;

import jakarta.persistence.*;

/*
 * ServizioAggiuntivo è un servizio opzionale dello stabilimento (livello Entity,
 * BCED): es. lettini extra, cabine, parcheggio.
 *
 * "capacita" è il tetto massimo di disponibilità deciso dal gestore (dato statico
 * di configurazione, richiesto dalla specifica come "disponibilità limitata").
 * La disponibilità residua per una certa data è invece un dato derivato dalle
 * prenotazioni attive (casi d'uso successivi), e non è memorizzata qui.
 */
@Entity
public class ServizioAggiuntivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descrizione;

    private int capacita;

    public ServizioAggiuntivo() {
    }

    public ServizioAggiuntivo(String descrizione, int capacita) {
        this.descrizione = descrizione;
        this.capacita = capacita;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public int getCapacita() {
        return capacita;
    }

    public void setCapacita(int capacita) {
        this.capacita = capacita;
    }

    @Override
    public String toString() {
        return "ServizioAggiuntivo{id=" + id + ", descrizione='" + descrizione
                + "', capacita=" + capacita + '}';
    }
}

package entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/*
 * ServizioPrenotato è la classe associativa dell'associazione "include" tra
 * Prenotazione e ServizioAggiuntivo: rappresenta una riga "quel servizio, in
 * quella prenotazione, in quella quantità".
 */
@Entity
@IdClass(ServizioPrenotato.Chiave.class)
public class ServizioPrenotato {

    @Id
    @ManyToOne
    @JoinColumn(name = "prenotazione_id")
    private Prenotazione prenotazione;

    @Id
    @ManyToOne
    @JoinColumn(name = "servizio_id")
    private ServizioAggiuntivo servizio;

    private int quantita;

    public ServizioPrenotato() {
    }

    public ServizioPrenotato(Prenotazione prenotazione, ServizioAggiuntivo servizio, int quantita) {
        this.prenotazione = prenotazione;
        this.servizio = servizio;
        this.quantita = quantita;
    }

    public Prenotazione getPrenotazione() {
        return prenotazione;
    }

    public void setPrenotazione(Prenotazione prenotazione) {
        this.prenotazione = prenotazione;
    }

    public ServizioAggiuntivo getServizio() {
        return servizio;
    }

    public void setServizio(ServizioAggiuntivo servizio) {
        this.servizio = servizio;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    @Override
    public String toString() {
        return "ServizioPrenotato{servizio="
                + (servizio != null ? servizio.getDescrizione() : null)
                + ", quantita=" + quantita + '}';
    }

    /*
     * IdClass della chiave composta: POJO serializable con i nomi dei campi @Id
     * (prenotazione, servizio) e il tipo della chiave primaria delle entità
     * riferite (Long). Per le regole JPA deve definire equals() e hashCode().
     */
    public static class Chiave implements Serializable {

        private Long prenotazione;
        private Long servizio;

        public Chiave() {
        }

        public Chiave(Long prenotazione, Long servizio) {
            this.prenotazione = prenotazione;
            this.servizio = servizio;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Chiave)) {
                return false;
            }
            Chiave altro = (Chiave) o;
            return Objects.equals(prenotazione, altro.prenotazione)
                    && Objects.equals(servizio, altro.servizio);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prenotazione, servizio);
        }
    }
}

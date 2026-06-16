package entity;

import jakarta.persistence.*;

/*
 * Tariffa è la superclasse astratta delle tariffe e rappresenta un costo valido per una certa stagione.
 * La estendono TariffaTipoFila (prezzo di un tipo di fila)
 * e TariffaServizioAggiuntivo (prezzo di un servizio).
 * La mappatura dell'ereditarietà è con strategia SINGLE_TABLE (come per Utente):
 * Tutte le sottoclassi stanno nella stessa tabella, con colonna discriminante "tipo_tariffa" (TIPO_FILA/SERVIZIO)
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_tariffa", discriminatorType = DiscriminatorType.STRING)
public abstract class Tariffa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double costo;

    @Enumerated(EnumType.STRING)
    private Stagione stagione;

    public Tariffa() {
    }

    public Tariffa(double costo, Stagione stagione) {
        this.costo = costo;
        this.stagione = stagione;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getCosto() {
        return costo;
    }

    public void setCosto(double costo) {
        this.costo = costo;
    }

    public Stagione getStagione() {
        return stagione;
    }

    public void setStagione(Stagione stagione) {
        this.stagione = stagione;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id
                + ", costo=" + costo + ", stagione=" + stagione + '}';
    }
}

package entity;

import jakarta.persistence.*;

/*
 * Ombrellone è la singola postazione, appartiene a una FilaOmbrelloni ed
 * è numerato progressivamente all'interno della propria fila.
 * La disponibilità per una certa data è un dato derivato dalle prenotazioni attive.
 */
@Entity
public class Ombrellone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero;

    @ManyToOne
    @JoinColumn(name = "fila_id")
    private FilaOmbrelloni fila;

    public Ombrellone() {
    }

    public Ombrellone(int numero, FilaOmbrelloni fila) {
        this.numero = numero;
        this.fila = fila;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public FilaOmbrelloni getFila() {
        return fila;
    }

    public void setFila(FilaOmbrelloni fila) {
        this.fila = fila;
    }

    @Override
    public String toString() {
        return "Ombrellone{id=" + id + ", numero=" + numero + '}';
    }
}

package entity;

import jakarta.persistence.*;

/*
 * Ombrellone è la singola postazione (livello Entity, BCED). È la "foglia" della
 * struttura dello stabilimento: appartiene a una FilaOmbrelloni (composizione) ed
 * è numerato progressivamente all'interno della propria fila.
 *
 * La disponibilità per una certa data NON è uno stato memorizzato qui: sarà un
 * dato derivato dalle prenotazioni attive (casi d'uso successivi). Per questo
 * l'ombrellone non ha un attributo "stato".
 */
@Entity
public class Ombrellone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Numero progressivo dell'ombrellone all'interno della sua fila (1..N).
     */
    private int numero;

    /*
     * La fila a cui appartiene l'ombrellone. Lato "molti" della composizione
     * FilaOmbrelloni ◆— Ombrellone.
     */
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
        // Non si stampa la fila per evitare ricorsione nel toString.
        return "Ombrellone{id=" + id + ", numero=" + numero + '}';
    }
}

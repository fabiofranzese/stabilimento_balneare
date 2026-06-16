package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/*
 * FilaOmbrelloni è una fila di ombrelloni dello stabilimento.
 * Questa aggrega i propri Ombrellone in composizione
 */
@Entity
public class FilaOmbrelloni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero;

    /*
     * Posizione della fila (prima / intermedia / ultima).
     */
    @Enumerated(EnumType.STRING)
    private TipoFila tipoFila;

    @OneToMany(mappedBy = "fila", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Ombrellone> ombrelloni = new ArrayList<>();

    public FilaOmbrelloni() {
    }

    public FilaOmbrelloni(int numero, TipoFila tipoFila) {
        this.numero = numero;
        this.tipoFila = tipoFila;
    }

    /*
     * Crea un Ombrellone con il numero indicato, lo collega a questa fila e lo aggiunge alla collezione.
     */
    public Ombrellone creaOmbrellone(int numero) {
        Ombrellone ombrellone = new Ombrellone(numero, this);
        ombrelloni.add(ombrellone);
        return ombrellone;
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

    public TipoFila getTipoFila() {
        return tipoFila;
    }

    public void setTipoFila(TipoFila tipoFila) {
        this.tipoFila = tipoFila;
    }

    public List<Ombrellone> getOmbrelloni() {
        return ombrelloni;
    }

    public void setOmbrelloni(List<Ombrellone> ombrelloni) {
        this.ombrelloni = ombrelloni;
    }

    @Override
    public String toString() {
        return "FilaOmbrelloni{id=" + id + ", numero=" + numero
                + ", tipoFila=" + tipoFila + ", ombrelloni=" + ombrelloni.size() + '}';
    }
}

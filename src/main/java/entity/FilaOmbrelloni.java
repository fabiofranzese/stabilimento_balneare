package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/*
 * FilaOmbrelloni è una fila di ombrelloni dello stabilimento (livello Entity,
 * BCED). Aggrega i propri Ombrellone in composizione (◆—): gli ombrelloni
 * vivono e sono numerati all'interno della fila.
 *
 * Ruolo GRASP: «Ombrellone Creator». Poiché contiene gli ombrelloni, è la fila
 * a istanziarli (creaOmbrellone): chi aggrega crea.
 *
 * La collezione è EAGER perché GestorePersistenza chiude l'EntityManager al
 * termine di ogni operazione: gli ombrelloni vanno caricati subito, prima della
 * chiusura, per poter essere letti dai livelli superiori senza errori di lazy
 * loading.
 */
@Entity
public class FilaOmbrelloni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Numero progressivo della fila nello stabilimento (1..N).
     */
    private int numero;

    /*
     * Posizione della fila (prima / intermedia / ultima). Tutti gli ombrelloni
     * della fila ne ereditano la posizione.
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
     * «Creator»: crea un Ombrellone con il numero indicato, lo collega a questa
     * fila e lo aggiunge alla collezione. Restituisce l'ombrellone creato.
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

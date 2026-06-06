package entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/*
 * Prenotazione è la prenotazione di un ombrellone per una certa data (livello
 * Entity, BCED). Lo stato è modellato col pattern State (StatoPrenotazione).
 *
 * In questo caso d'uso (Visualizzazione Mappa) della Prenotazione interessano
 * solo i campi necessari al calcolo della disponibilità: data, ombrellone e
 * stato. Il cliente, i servizi aggiuntivi e la data di creazione verranno
 * aggiunti nel caso d'uso Effettua Prenotazione, quando le prenotazioni vengono
 * effettivamente create.
 *
 * Nota di dominio: la disponibilità di un ombrellone NON è un suo attributo, ma
 * un dato derivato: l'ombrellone è occupato in una data se esiste una
 * prenotazione attiva (stato Prenotata) che lo riguarda in quella data.
 */
@Entity
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Giorno prenotato.
     */
    private LocalDate data;

    @ManyToOne
    @JoinColumn(name = "ombrellone_id")
    private Ombrellone ombrellone;

    @ManyToOne
    @JoinColumn(name = "stato_id")
    private StatoPrenotazione stato;

    public Prenotazione() {
    }

    public Prenotazione(LocalDate data, Ombrellone ombrellone, StatoPrenotazione stato) {
        this.data = data;
        this.ombrellone = ombrellone;
        this.stato = stato;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Ombrellone getOmbrellone() {
        return ombrellone;
    }

    public void setOmbrellone(Ombrellone ombrellone) {
        this.ombrellone = ombrellone;
    }

    public StatoPrenotazione getStato() {
        return stato;
    }

    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
    }

    @Override
    public String toString() {
        return "Prenotazione{id=" + id + ", data=" + data
                + ", ombrellone=" + (ombrellone != null ? ombrellone.getNumero() : null)
                + ", stato=" + (stato != null ? stato.nome() : null) + '}';
    }
}

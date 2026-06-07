package entity;

import jakarta.persistence.*;

/*
 * StatoPrenotazione è la radice del pattern State applicato alla Prenotazione
 * (livello Entity, BCED). Le sottoclassi concrete Prenotata e Annullata
 * incapsulano il comportamento che varia con lo stato.
 *
 * Mappatura: come Utente e Tariffa, si usa l'ereditarietà SINGLE_TABLE con
 * colonna discriminante "tipo_stato". Gli stati sono pochi e condivisi: vengono
 * predisposti come righe "singleton" (vedi setup.DatiIniziali) e referenziati da
 * ogni Prenotazione tramite @ManyToOne.
 *
 * NOTE: in questo caso d'uso (Visualizzazione Mappa) si usa solo la lettura dello
 * stato (per il calcolo della disponibilità). Le transizioni di stato (creazione
 * in "prenotata", annullamento) sono introdotte nei casi d'uso successivi.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_stato", discriminatorType = DiscriminatorType.STRING)
public abstract class StatoPrenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Una prenotazione è "attiva" se occupa effettivamente la postazione: solo
     * gli stati attivi rendono un ombrellone non disponibile per una data.
     */
    public abstract boolean isAttiva();

    /*
     * Comportamento che varia con lo stato (pattern State): indica se da questo
     * stato è ammessa la transizione verso "annullata". Solo una prenotazione
     * ancora attiva (Prenotata) può essere annullata; una già Annullata no.
     */
    public abstract boolean isAnnullabile();

    /*
     * Nome leggibile dello stato (per la GUI).
     */
    public abstract String nome();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + '}';
    }
}

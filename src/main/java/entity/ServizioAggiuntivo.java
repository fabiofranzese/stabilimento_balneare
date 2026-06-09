package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/*
 * ServizioAggiuntivo è un servizio opzionale dello stabilimento (livello Entity,
 * BCED): es. lettini extra, cabine, parcheggio.
 *
 * "capacita" è il tetto massimo di disponibilità deciso dal gestore (dato statico
 * di configurazione, richiesto dalla specifica come "disponibilità limitata").
 * La disponibilità residua per una certa data è invece un dato derivato dalle
 * prenotazioni attive, e non è memorizzata qui.
 */
@Entity
public class ServizioAggiuntivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descrizione;

    private int capacita;

    /*
     * Le tariffe del servizio (una per stagione). Il servizio "possiede" le sue
     * tariffe: eliminando il servizio (es. riconfigurando lo stabilimento) le
     * relative tariffe vengono rimosse a cascata, senza restare orfane.
     */
    @OneToMany(mappedBy = "servizio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TariffaServizioAggiuntivo> tariffe = new ArrayList<>();

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

    public List<TariffaServizioAggiuntivo> getTariffe() {
        return tariffe;
    }

    public void setTariffe(List<TariffaServizioAggiuntivo> tariffe) {
        this.tariffe = tariffe;
    }

    @Override
    public String toString() {
        return "ServizioAggiuntivo{id=" + id + ", descrizione='" + descrizione
                + "', capacita=" + capacita + '}';
    }
}

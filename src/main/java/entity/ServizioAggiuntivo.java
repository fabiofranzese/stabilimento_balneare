package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/*
 * ServizioAggiuntivo è un servizio opzionale dello stabilimento (es. lettini extra, cabine, parcheggio).
 *
 * "capacita" è il tetto massimo di disponibilità deciso dal gestore.
 * La disponibilità residua per una certa data è un dato derivato dalle prenotazioni attive.
 */
@Entity
public class ServizioAggiuntivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descrizione;

    private int capacita;

    /*
     * Tariffe del servizio, una per stagione.
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

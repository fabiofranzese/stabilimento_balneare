package entity;

import jakarta.persistence.*;

/*
 * TariffaServizioAggiuntivo è il prezzo di un servizio aggiuntivo per una certa stagione.
 * Questa è sottoclasse concreta di Tariffa con discriminatore "SERVIZIO".
 */
@Entity
@DiscriminatorValue("SERVIZIO")
public class TariffaServizioAggiuntivo extends Tariffa {

    @ManyToOne
    @JoinColumn(name = "servizio_id")
    private ServizioAggiuntivo servizio;

    public TariffaServizioAggiuntivo() {
    }

    public TariffaServizioAggiuntivo(ServizioAggiuntivo servizio, double costo, Stagione stagione) {
        super(costo, stagione);
        this.servizio = servizio;
    }

    public ServizioAggiuntivo getServizio() {
        return servizio;
    }

    public void setServizio(ServizioAggiuntivo servizio) {
        this.servizio = servizio;
    }
}

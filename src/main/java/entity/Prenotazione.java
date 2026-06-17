package entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Prenotazione di un ombrellone per una data, effettuata da un Cliente, con zero o più servizi aggiuntivi.
 * Lo stato è modellato col pattern State (StatoPrenotazione).
 */
@Entity
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data;
    private LocalDateTime dataCreazione;

    /*
     * Prezzo totale "congelato" alla creazione in base alle tariffe di quel momento.
     */
    private double prezzoTotale;

    @ManyToOne
    @JoinColumn(name = "ombrellone_id")
    private Ombrellone ombrellone;

    /*
     * Stato della prenotazione (pattern State)
     */
    @Column(name = "tipo_stato")
    private String tipoStato;

    @Transient
    private StatoPrenotazione stato;

    /*
     * Il cliente che ha effettuato la prenotazione
     */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /*
     * Servizi aggiuntivi inclusi con la quantità prenotata per ciascuno.
     */
    @OneToMany(mappedBy = "prenotazione", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ServizioPrenotato> serviziPrenotati = new ArrayList<>();

    public Prenotazione() {
    }

    public Prenotazione(LocalDate data, Ombrellone ombrellone, StatoPrenotazione stato,
                        Cliente cliente, Map<ServizioAggiuntivo, Integer> quantitaServizi,
                        LocalDateTime dataCreazione, double prezzoTotale) {
        this.data = data;
        this.ombrellone = ombrellone;
        setStato(stato);
        this.cliente = cliente;
        if (quantitaServizi != null) {
            for (Map.Entry<ServizioAggiuntivo, Integer> voce : quantitaServizi.entrySet()) {
                aggiungiServizio(voce.getKey(), voce.getValue() != null ? voce.getValue() : 0);
            }
        }
        this.dataCreazione = dataCreazione;
        this.prezzoTotale = prezzoTotale;
    }

    /*
     * Crea una riga ServizioPrenotato collegata a questa prenotazione e la aggiunge alla collezione.
     */
    public ServizioPrenotato aggiungiServizio(ServizioAggiuntivo servizio, int quantita) {
        ServizioPrenotato riga = new ServizioPrenotato(this, servizio, quantita);
        serviziPrenotati.add(riga);
        return riga;
    }

    public boolean isAnnullabile(LocalDate oggi) {
        return stato != null && stato.isAnnullabile()
                && oggi != null && data != null && oggi.isBefore(data);
    }

    /*
     * Evento "annulla" del pattern State: il Context delega allo stato corrente,
     * che decide la transizione (Prenotata -> Annullata) o la ignora (Annullata).
     */
    public void annulla() {
        if (stato != null) {
            stato.annulla(this);
        }
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

    /*
     * Imposta lo stato e ne allinea la rappresentazione persistente "tipo_stato".
     */
    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
        this.tipoStato = (stato instanceof Annullata) ? "ANNULLATA"
                : (stato != null ? "PRENOTATA" : null);
    }

    /*
     * Dopo il caricamento dal DB ricostruisce l'istanza concreta dello stato dal
     * "tipo_stato" salvato (lo stato è @Transient).
     */
    @PostLoad
    private void ricostruisciStato() {
        if (tipoStato == null) {
            stato = null;
        } else {
            stato = "ANNULLATA".equals(tipoStato) ? new Annullata() : new Prenotata();
        }
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<ServizioPrenotato> getServiziPrenotati() {
        return serviziPrenotati;
    }

    public void setServiziPrenotati(List<ServizioPrenotato> serviziPrenotati) {
        this.serviziPrenotati = serviziPrenotati;
    }

    public double getPrezzoTotale() {
        return prezzoTotale;
    }

    public void setPrezzoTotale(double prezzoTotale) {
        this.prezzoTotale = prezzoTotale;
    }

    @Override
    public String toString() {
        return "Prenotazione{id=" + id + ", data=" + data
                + ", ombrellone=" + (ombrellone != null ? ombrellone.getNumero() : null)
                + ", stato=" + (stato != null ? stato.nome() : null)
                + ", cliente=" + (cliente != null ? cliente.getEmail() : null)
                + ", servizi=" + serviziPrenotati.size()
                + ", prezzoTotale=" + prezzoTotale + '}';
    }
}

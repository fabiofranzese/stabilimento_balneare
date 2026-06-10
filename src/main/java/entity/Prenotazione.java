package entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Prenotazione di un ombrellone per un giorno (data), effettuata da un Cliente,
 * con zero o più servizi aggiuntivi (livello Entity, BCED). Lo stato è modellato
 * col pattern State (StatoPrenotazione).
 *
 * La disponibilità di un ombrellone non è un suo attributo: è un dato derivato,
 * l'ombrellone è occupato in una data se esiste una prenotazione attiva (stato
 * Prenotata) che lo riguarda in quella data.
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

    /*
     * Istante di creazione della prenotazione (dato di registrazione per lo storico).
     */
    private LocalDateTime dataCreazione;

    /*
     * Prezzo totale "congelato" alla creazione, alle tariffe vigenti in quel
     * momento: lo storico mostra il prezzo effettivamente applicato al cliente,
     * indipendente da successive modifiche delle tariffe.
     */
    private double prezzoTotale;

    @ManyToOne
    @JoinColumn(name = "ombrellone_id")
    private Ombrellone ombrellone;

    /*
     * Stato della prenotazione (pattern State). Lo stato è un oggetto puramente
     * comportamentale, non una Entity: se ne persiste solo il tipo nella colonna
     * "tipo_stato", mentre l'istanza concreta Prenotata/Annullata è @Transient e
     * viene ricostruita in lettura (@PostLoad). setStato tiene allineate le due
     * rappresentazioni.
     */
    @Column(name = "tipo_stato")
    private String tipoStato;

    @Transient
    private StatoPrenotazione stato;

    /*
     * Il cliente che ha effettuato la prenotazione (lato proprietario
     * dell'associazione "ha effettuato").
     */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /*
     * Servizi aggiuntivi inclusi, con la quantità prenotata per ciascuno
     * (associazione "include" con attributo quantità): Map con chiave la Entity
     * ServizioAggiuntivo, senza una classe associativa dedicata. EAGER perché
     * GestorePersistenza chiude l'EntityManager al termine di ogni operazione.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prenotazione_servizio",
            joinColumns = @JoinColumn(name = "prenotazione_id"))
    @MapKeyJoinColumn(name = "servizio_id")
    @Column(name = "quantita")
    private Map<ServizioAggiuntivo, Integer> quantitaServizi = new LinkedHashMap<>();

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
            this.quantitaServizi = quantitaServizi;
        }
        this.dataCreazione = dataCreazione;
        this.prezzoTotale = prezzoTotale;
    }

    /*
     * Information Expert del limite di annullamento: la prenotazione è annullabile
     * se lo stato lo consente (solo una Prenotata) e la richiesta avviene entro il
     * limite temporale, cioè prima del giorno prenotato (oggi < data).
     */
    public boolean isAnnullabile(LocalDate oggi) {
        return stato != null && stato.isAnnullabile()
                && oggi != null && data != null && oggi.isBefore(data);
    }

    /*
     * Evento "annulla" del pattern State: il Context delega allo stato corrente,
     * che decide la transizione (Prenotata -> Annullata) o la ignora (Annullata).
     *
     * NOTE: la precondizione temporale (oggi < data) è verificata a monte dal
     * Controller tramite isAnnullabile(oggi); qui lo stato gestisce la sola
     * validità di stato.
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
     * Imposta lo stato e ne allinea la rappresentazione persistente "tipo_stato":
     * è l'unico punto che traduce l'istanza nel suo tipo, così le due viste dello
     * stesso stato non possono divergere.
     */
    public void setStato(StatoPrenotazione stato) {
        this.stato = stato;
        this.tipoStato = (stato instanceof Annullata) ? "ANNULLATA"
                : (stato != null ? "PRENOTATA" : null);
    }

    /*
     * Dopo il caricamento dal DB ricostruisce l'istanza concreta dello stato dal
     * "tipo_stato" salvato (lo stato è @Transient): è il verso di lettura del
     * pattern State persistito come singola colonna.
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

    public Map<ServizioAggiuntivo, Integer> getQuantitaServizi() {
        return quantitaServizi;
    }

    public void setQuantitaServizi(Map<ServizioAggiuntivo, Integer> quantitaServizi) {
        this.quantitaServizi = quantitaServizi;
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
                + ", servizi=" + quantitaServizi.size()
                + ", prezzoTotale=" + prezzoTotale + '}';
    }
}

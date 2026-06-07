package entity;

/*
 * ServizioEsauritoException segnala che, per la data scelta, la disponibilità
 * residua di un servizio aggiuntivo selezionato è esaurita (residuo <= 0).
 *
 * Eccezione di dominio (livello Entity, BCED): lanciata da
 * RegistroPrenotazioni.effettuaPrenotazione (estensione 3.1.a del flusso) e
 * mappata a un codice di esito dal Controller. Porta con sé il servizio in
 * conflitto, utile per la diagnostica.
 */
public class ServizioEsauritoException extends RuntimeException {

    private final ServizioAggiuntivo servizio;

    public ServizioEsauritoException(ServizioAggiuntivo servizio) {
        super("Servizio esaurito: " + (servizio != null ? servizio.getDescrizione() : null));
        this.servizio = servizio;
    }

    public ServizioAggiuntivo getServizio() {
        return servizio;
    }
}

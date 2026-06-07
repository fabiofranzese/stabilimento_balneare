package entity;

/*
 * OmbrelloneNonDisponibileException segnala che l'ombrellone richiesto risulta
 * già occupato (da un'altra prenotazione attiva) per la data scelta.
 *
 * Eccezione di dominio (livello Entity, BCED): lanciata da
 * RegistroPrenotazioni.effettuaPrenotazione quando il controllo dei conflitti
 * fallisce (estensione 2.a del flusso), e mappata a un codice di esito dal
 * Controller. È unchecked per non costringere le firme dei Registro a dichiararla.
 */
public class OmbrelloneNonDisponibileException extends RuntimeException {

    public OmbrelloneNonDisponibileException(String messaggio) {
        super(messaggio);
    }
}

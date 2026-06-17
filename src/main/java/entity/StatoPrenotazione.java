package entity;

/*
 * Pattern State: interfaccia dello stato di una Prenotazione (Context).
 * Espone solo l'evento annulla(); ogni stato concreto decide se eseguire la
 * transizione (Prenotata -> Annullata via setStato) o ignorare l'evento.
 */
public interface StatoPrenotazione {

    /*
     * Evento "annulla". Riceve la Prenotazione (Context) per poterne cambiare lo
     * stato corrente con prenotazione.setStato(...).
     */
    void annulla(Prenotazione prenotazione);
}

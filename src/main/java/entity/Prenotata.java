package entity;

/*
 * Stato concreto di una prenotazione attiva.
 */
public class Prenotata implements StatoPrenotazione {

    /*
     * Dallo stato "prenotata" l'annullamento esegue la transizione: lo stato
     * concreto costruisce il successore e lo assegna al Context.
     */
    @Override
    public void annulla(Prenotazione prenotazione) {
        prenotazione.setStato(new Annullata());
    }
}

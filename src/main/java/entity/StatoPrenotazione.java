package entity;

/*
 * StatoPrenotazione è la radice del pattern State applicato alla Prenotazione
 * (livello Entity, BCED). Le sottoclassi concrete Prenotata e Annullata
 * incapsulano il comportamento che varia con lo stato.
 *
 * Oggetto puramente comportamentale, NON una Entity JPA: come nell'esempio
 * Porta/StatoPorta del docente, lo stato non ha una tabella propria. La sua
 * persistenza è ridotta a una sola colonna "tipo_stato" sulla tabella
 * Prenotazione: in scrittura se ne salva il tipo (Prenotazione.setStato), in
 * lettura l'istanza concreta è ricostruita da Prenotazione (@PostLoad).
 */
public abstract class StatoPrenotazione {

    /*
     * Una prenotazione è "attiva" se occupa effettivamente la postazione: solo
     * gli stati attivi rendono un ombrellone non disponibile per una data.
     */
    public abstract boolean isAttiva();

    /*
     * Comportamento che varia con lo stato (pattern State): indica se da questo
     * stato è ammessa la transizione verso "annullata". Solo una prenotazione
     * ancora attiva (Prenotata) può essere annullata; una già Annullata no.
     * Usata dalla GUI (per abilitare "Annulla") e dal Controller (per l'esito).
     */
    public abstract boolean isAnnullabile();

    /*
     * Evento "annulla" del pattern State: la Prenotazione (Context) delega allo
     * stato corrente. Ogni stato concreto decide la transizione — Prenotata passa
     * ad Annullata (porta.setStato(new Annullata())), Annullata è un no-op
     * ("nessuna transizione prevista"). Mirroring dell'esempio Porta/StatoPorta.
     */
    public abstract void annulla(Prenotazione prenotazione);

    /*
     * Nome leggibile dello stato (per la GUI).
     */
    public abstract String nome();

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

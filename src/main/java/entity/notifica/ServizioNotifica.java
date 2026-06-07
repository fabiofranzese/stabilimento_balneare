package entity.notifica;

import entity.Prenotazione;

/*
 * ServizioNotifica è l'interfaccia del Servizio di Notifica, di proprietà del
 * livello Entity (BCED).
 *
 * Ricopre due ruoli nei pattern del progetto:
 * - Observer: è l'osservatore del ciclo di vita della prenotazione. Il Subject
 *   (RegistroPrenotazioni) la notifica quando una prenotazione viene effettuata.
 * - Target dell'Adapter: l'implementazione concreta (in infrastructure) è un
 *   Adapter che adatta un canale di comunicazione esterno a questa interfaccia.
 *
 * Tenere l'interfaccia in Entity realizza l'inversione delle dipendenze: il
 * dominio definisce "cosa" notificare senza dipendere dal "come" (il canale
 * concreto), che vive in infrastructure. Entity non importa infrastructure;
 * il collegamento avviene nel composition root (setup.Main).
 */
public interface ServizioNotifica {

    /*
     * Notifica che una prenotazione è stata effettuata, passando l'oggetto da
     * cui ricavare i dati da comunicare (cliente, ombrellone, data, servizi).
     */
    void prenotazioneEffettuata(Prenotazione prenotazione);
}

package boundary.notifica;

import notifica.CanaleComunicazioneEsterno;

/*
 * AdapterServizioNotifica è l'Adapter che gestisce l'interazione con il Servizio di Notifica esterno,
 * adattando il CanaleComunicazioneEsterno (COTS nel package `notifica`) alle notifiche dell'applicazione
 */
public class AdapterServizioNotifica {

    private final CanaleComunicazioneEsterno canale;

    public AdapterServizioNotifica(CanaleComunicazioneEsterno canale) {
        this.canale = canale;
    }

    public void prenotazioneEffettuata(String destinatario, String corpo) {
        canale.invia(destinatario, "Conferma prenotazione", corpo);
    }

    public void prenotazioneAnnullata(String destinatario, String corpo) {
        canale.invia(destinatario, "Annullamento prenotazione", corpo);
    }
}

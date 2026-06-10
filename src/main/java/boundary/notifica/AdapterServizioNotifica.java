package boundary.notifica;

import notifica.CanaleComunicazioneEsterno;

/*
 * AdapterServizioNotifica è l'Adapter (pattern omonimo) collocato nel Boundary
 * (BCED): gestisce l'interazione con il Servizio di Notifica esterno, adattando
 * il CanaleComunicazioneEsterno (adaptee, COTS nel package `notifica`) alle
 * notifiche dell'applicazione.
 *
 * I form lo invocano alla conferma dell'operazione (prenotazione effettuata o
 * annullata) passando destinatario e corpo del messaggio (testo composto dal
 * Controller); l'Adapter aggiunge l'oggetto dell'evento e traduce il tutto nella
 * chiamata invia(destinatario, oggetto, corpo) del canale. Riceve solo String:
 * nessuna Entity, nessun oggetto di trasferimento dedicato.
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

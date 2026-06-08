package boundary.notifica;

import notifica.CanaleComunicazioneEsterno;

/*
 * AdapterServizioNotifica è l'Adapter del pattern omonimo, collocato nel Boundary
 * (BCED): è la classe che gestisce l'interazione del sistema con il Servizio di
 * Notifica esterno. Adatta un CanaleComunicazioneEsterno (adaptee, COTS nel package
 * `notifica`) alle notifiche dell'applicazione.
 *
 * È invocato dal Boundary (i form) quando questo riceve la conferma dell'operazione
 * (prenotazione effettuata o annullata): riceve il destinatario e il corpo del
 * messaggio (testo già composto dal Controller, che dispone dei dati di dominio),
 * vi aggiunge l'oggetto specifico dell'evento e traduce il tutto nella chiamata
 * invia(destinatario, oggetto, corpo) del canale.
 *
 * Dipendenze: Boundary -> canale esterno (COTS). Riceve solo primitivi (String):
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

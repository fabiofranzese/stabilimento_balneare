package setup;

import boundary.FinestraPrincipale;
import entity.RegistroPrenotazioni;
import infrastructure.notifica.AdattatoreServizioNotifica;
import infrastructure.notifica.CanaleComunicazioneEsterno;

import javax.swing.*;

/*
 * Punto di ingresso dell'applicazione.
 *
 * Predispone i dati iniziali (account Gestore, stati delle prenotazioni), collega
 * il Servizio di Notifica al ciclo di vita delle prenotazioni (Observer/Adapter) e
 * avvia l'interfaccia grafica Swing sul thread di gestione degli eventi (Event
 * Dispatch Thread).
 */
public class Main {

    public static void main(String[] args) {
        // Crea l'account Gestore predefinito (la registrazione è riservata ai Cliente).
        DatiIniziali.seedGestore();

        // Predispone gli stati delle prenotazioni (Prenotata, Annullata).
        DatiIniziali.seedStatiPrenotazione();

        // Composition root: registra il Servizio di Notifica (Adapter su canale
        // esterno) come osservatore del RegistroPrenotazioni (Subject). È l'unico
        // punto in cui entity e infrastructure si incontrano: l'inversione delle
        // dipendenze tiene il dominio libero da import verso l'infrastruttura.
        RegistroPrenotazioni.aggiungiOsservatore(
                new AdattatoreServizioNotifica(new CanaleComunicazioneEsterno()));

        // Le componenti Swing vanno create e mostrate sull'Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new FinestraPrincipale().apri());
    }
}

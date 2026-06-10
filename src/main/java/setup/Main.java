package setup;

import boundary.FinestraPrincipale;

import javax.swing.*;

/*
 * Punto di ingresso dell'applicazione.
 *
 * Predispone i dati iniziali (account Gestore) e avvia l'interfaccia grafica Swing
 * sul thread di gestione degli eventi (Event Dispatch Thread).
 */
public class Main {

    public static void main(String[] args) {
        // Crea l'account Gestore predefinito (la registrazione è riservata ai Cliente).
        DatiIniziali.seedGestore();

        // Le componenti Swing vanno create e mostrate sull'Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new FinestraPrincipale().apri());
    }
}

package infrastructure.notifica;

/*
 * CanaleComunicazioneEsterno simula un canale di comunicazione esterno (email /
 * SMS) verso cui inviare le notifiche (livello Infrastructure).
 *
 * Rappresenta l'"adaptee" del pattern Adapter: ha una propria interfaccia
 * (invia(destinatario, oggetto, corpo)) che non coincide con quella attesa dal
 * dominio (ServizioNotifica). L'AdattatoreServizioNotifica fa da ponte tra le due.
 *
 * Qui l'invio è simulato con una stampa a console: sostituendo questa classe (o
 * iniettando un canale reale nell'Adapter) si cambia il mezzo senza toccare il
 * dominio.
 */
public class CanaleComunicazioneEsterno {

    /*
     * Invia un messaggio sul canale esterno. Firma volutamente diversa da quella
     * di ServizioNotifica: è ciò che l'Adapter deve adattare.
     */
    public void invia(String destinatario, String oggetto, String corpo) {
        System.out.println("========== NOTIFICA (canale esterno) ==========");
        System.out.println("A: " + destinatario);
        System.out.println("Oggetto: " + oggetto);
        System.out.println(corpo);
        System.out.println("===============================================");
    }
}

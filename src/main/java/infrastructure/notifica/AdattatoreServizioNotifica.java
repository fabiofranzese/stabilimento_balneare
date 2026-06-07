package infrastructure.notifica;

import entity.Cliente;
import entity.Prenotazione;
import entity.ServizioAggiuntivo;
import entity.notifica.ServizioNotifica;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/*
 * AdattatoreServizioNotifica è l'Adapter del pattern omonimo (livello
 * Infrastructure): adatta un CanaleComunicazioneEsterno (adaptee) all'interfaccia
 * ServizioNotifica attesa dal dominio (target, di proprietà di Entity).
 *
 * Funge anche da osservatore concreto nel pattern Observer: il
 * RegistroPrenotazioni (Subject) lo notifica quando una prenotazione viene
 * effettuata. L'Adapter ricava dalla Prenotazione i dati da comunicare e li
 * traduce nella chiamata invia(destinatario, oggetto, corpo) del canale.
 *
 * La dipendenza è verso l'interno (infrastructure -> entity): è il dominio a
 * definire l'interfaccia, l'infrastruttura a implementarla (inversione delle
 * dipendenze). Il collegamento Subject <-> Adapter avviene in setup.Main.
 */
public class AdattatoreServizioNotifica implements ServizioNotifica {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CanaleComunicazioneEsterno canale;

    public AdattatoreServizioNotifica(CanaleComunicazioneEsterno canale) {
        this.canale = canale;
    }

    @Override
    public void prenotazioneEffettuata(Prenotazione prenotazione) {
        String destinatario = destinatario(prenotazione.getCliente());
        String oggetto = "Conferma prenotazione";
        String corpo = componiCorpo(prenotazione);

        canale.invia(destinatario, oggetto, corpo);
    }

    /*
     * Recapito del cliente: si usa l'email; in mancanza, il telefono.
     */
    private String destinatario(Cliente cliente) {
        if (cliente == null) {
            return "(destinatario sconosciuto)";
        }
        if (cliente.getEmail() != null && !cliente.getEmail().isEmpty()) {
            return cliente.getEmail();
        }
        return cliente.getTelefono();
    }

    /*
     * Testo della notifica con i dati della prenotazione (cliente, ombrellone,
     * data, servizi inclusi).
     */
    private String componiCorpo(Prenotazione prenotazione) {
        StringBuilder corpo = new StringBuilder();

        Cliente cliente = prenotazione.getCliente();
        if (cliente != null) {
            corpo.append("Gentile ").append(cliente.getNome()).append(' ')
                    .append(cliente.getCognome()).append(",\n");
        }

        corpo.append("la sua prenotazione è confermata.\n");

        if (prenotazione.getOmbrellone() != null) {
            corpo.append("Ombrellone n. ").append(prenotazione.getOmbrellone().getNumero()).append('\n');
        }
        if (prenotazione.getData() != null) {
            corpo.append("Data: ").append(prenotazione.getData().format(FORMATO_DATA)).append('\n');
        }

        if (!prenotazione.getQuantitaServizi().isEmpty()) {
            corpo.append("Servizi aggiuntivi:\n");
            for (Map.Entry<ServizioAggiuntivo, Integer> voce
                    : prenotazione.getQuantitaServizi().entrySet()) {
                corpo.append("  - ").append(voce.getValue()).append(" x ")
                        .append(voce.getKey().getDescrizione()).append('\n');
            }
        }

        corpo.append("Totale: ").append(String.format("€ %.2f", prenotazione.getPrezzoTotale()))
                .append('\n');

        return corpo.toString();
    }
}

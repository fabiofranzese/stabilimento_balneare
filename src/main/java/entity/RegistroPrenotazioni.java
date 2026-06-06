package entity;

import database.GestorePersistenza;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * RegistroPrenotazioni è il servizio di dominio per le prenotazioni (livello
 * Entity, BCED).
 *
 * Ruoli GRASP: Creator e Information Expert delle Prenotazione. In questo caso
 * d'uso (Visualizzazione Mappa) ne serve la sola parte di lettura, su cui si basa
 * il calcolo della disponibilità; la creazione delle prenotazioni
 * (effettuaPrenotazione, con controllo dei conflitti) sarà aggiunta nel caso
 * d'uso Effettua Prenotazione.
 *
 * Usa GestorePersistenza (livello Database), come gli altri Registro.
 */
public class RegistroPrenotazioni {

    private GestorePersistenza gestorePersistenza;

    public RegistroPrenotazioni() {
        gestorePersistenza = new GestorePersistenza();
    }

    /*
     * Information Expert: un ombrellone è occupato in una certa data se esiste
     * una prenotazione ATTIVA (stato Prenotata) che lo riguarda in quella data.
     * La disponibilità è quindi un dato derivato, non memorizzato sull'ombrellone.
     */
    public boolean isOmbrelloneOccupato(Ombrellone ombrellone, LocalDate data) {
        StatoPrenotazione prenotata = statoPrenotata();

        if (prenotata == null) {
            // Nessuno stato "Prenotata" registrato: non esistono prenotazioni attive.
            return false;
        }

        // Si usa una mappa ordinata per costruire i criteri della ricerca.
        Map<String, Object> criteri = new LinkedHashMap<>();
        criteri.put("ombrellone", ombrellone);
        criteri.put("data", data);
        criteri.put("stato", prenotata);

        return gestorePersistenza.cercaPrimoPerCampi(Prenotazione.class, criteri) != null;
    }

    /*
     * Restituisce lo stato "Prenotata" predisposto all'avvio (riga singleton),
     * oppure null se non presente.
     */
    private StatoPrenotazione statoPrenotata() {
        return gestorePersistenza.cercaPrimoPerCampi(Prenotata.class, Map.of());
    }
}

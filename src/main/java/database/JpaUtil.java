package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/*
 * JpaUtil applica il pattern Singleton al livello Database (BCED).
 *
 * Mantiene un'unica EntityManagerFactory condivisa per tutta l'applicazione,
 * creata a partire dalla persistence unit definita in persistence.xml.
 */
public class JpaUtil {

    /*
     * Istanza unica di JpaUtil: cuore del pattern Singleton.
     */
    private static JpaUtil instance;

    /*
     * EntityManagerFactory condivisa.
     *
     * La factory è costosa da inizializzare: legge la persistence unit dal file
     * persistence.xml e prepara Hibernate per comunicare con il database, perciò
     * viene creata una sola volta.
     */
    private EntityManagerFactory emf;

    /*
     * Costruttore privato: impedisce di creare oggetti JpaUtil con new.
     * L'unico modo per ottenere l'istanza è il metodo statico getInstance().
     */
    private JpaUtil() {
        /*
         * Il nome "stabilimentiPU" deve coincidere con quello dichiarato in
         * persistence.xml: <persistence-unit name="stabilimentiPU">
         */
        emf = Persistence.createEntityManagerFactory("stabilimentiPU");
    }

    /*
     * Punto di accesso globale all'unica istanza di JpaUtil.
     * Se non esiste ancora viene creata, altrimenti viene restituita quella esistente.
     */
    public static JpaUtil getInstance() {
        if (instance == null) {
            instance = new JpaUtil();
        }

        return instance;
    }

    /*
     * Crea un nuovo EntityManager.
     *
     * Attenzione: l'EntityManager non è Singleton. Ogni operazione di persistenza
     * deve usare un proprio EntityManager, perché mantiene lo stato della singola
     * sessione di lavoro con il database.
     */
    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /*
     * Chiude la EntityManagerFactory.
     * Va chiamato alla fine dell'applicazione, quando non servono più operazioni
     * di persistenza.
     */
    public void chiudi() {
        emf.close();
    }
}

package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/*
 * JpaUtil applica il pattern Singleton al livello Database:
 * Mantiene un'unica EntityManagerFactory condivisa per tutta l'applicazione.
 */
public class JpaUtil {

    /*
     * Istanza unica di JpaUtil.
     */
    private static JpaUtil instance;

    /*
     * EntityManagerFactory condivisa.
     */
    private EntityManagerFactory emf;

    /*
     * Costruttore privato: impedisce di creare oggetti JpaUtil con new.
     */
    private JpaUtil() {
        emf = Persistence.createEntityManagerFactory("stabilimentiPU");
    }

    /*
     * Punto di accesso globale all'unica istanza di JpaUtil.
     */
    public static JpaUtil getInstance() {
        if (instance == null) {
            instance = new JpaUtil();
        }

        return instance;
    }

    /*
     * Crea un nuovo EntityManager.
     */
    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /*
     * Chiude la EntityManagerFactory.
     */
    public void chiudi() {
        emf.close();
    }
}

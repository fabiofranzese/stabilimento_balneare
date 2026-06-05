package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Map;

/*
 * GestorePersistenza è il livello Database (BCED): incapsula tutti gli accessi
 * all'EntityManager e gestisce internamente le transazioni.
 *
 * È volutamente generico: non conosce le classi specifiche del dominio
 * (Cliente, Gestore, ...). Riceve e restituisce Entity, senza mai esporre
 * l'EntityManager verso i livelli superiori.
 */
public class GestorePersistenza {

    /*
     * Salva nel database un oggetto persistente (una Entity annotata con @Entity).
     * Restituisce true se l'operazione va a buon fine, false altrimenti.
     */
    public boolean salva(Object oggetto) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            // Ogni modifica al database avviene all'interno di una transazione.
            em.getTransaction().begin();

            // persist rende l'oggetto gestito: al commit diventa una riga della tabella.
            em.persist(oggetto);

            em.getTransaction().commit();
            return true;

        } catch (RuntimeException e) {
            // In caso di errore annulliamo la transazione per evitare modifiche parziali.
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            e.printStackTrace();
            return false;

        } finally {
            // L'EntityManager si chiude sempre; la factory resta aperta.
            em.close();
        }
    }

    /*
     * Salva più oggetti nella stessa transazione: o vengono salvati tutti,
     * oppure, in caso di errore, nessuno.
     */
    public boolean salvaTutti(Object... oggetti) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();

            for (Object oggetto : oggetti) {
                em.persist(oggetto);
            }

            em.getTransaction().commit();
            return true;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            e.printStackTrace();
            return false;

        } finally {
            em.close();
        }
    }

    /*
     * Cerca un oggetto persistente a partire dalla sua classe e dal suo id.
     * Generico: utilizzabile con qualunque Entity.
     */
    public <T> T trovaPerId(Class<T> classe, Long id) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            return em.find(classe, id);
        } finally {
            em.close();
        }
    }

    /*
     * Cerca tutti gli oggetti di una certa classe per cui un campo ha un dato valore.
     */
    public <T> List<T> cercaPerCampo(Class<T> classe, String nomeCampo, Object valore) {
        return cercaPerCampi(classe, Map.of(nomeCampo, valore));
    }

    /*
     * Cerca tutti gli oggetti che soddisfano un insieme di condizioni (campo = valore).
     * La query JPQL viene costruita qui, nel livello database.
     */
    public <T> List<T> cercaPerCampi(Class<T> classe, Map<String, Object> campi) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            StringBuilder jpql = new StringBuilder();

            jpql.append("SELECT e FROM ")
                    .append(classe.getSimpleName())
                    .append(" e");

            if (!campi.isEmpty()) {
                jpql.append(" WHERE ");

                int contatore = 0;

                for (String nomeCampo : campi.keySet()) {
                    if (contatore > 0) {
                        jpql.append(" AND ");
                    }

                    String nomeParametro = nomeCampo.replace(".", "_");

                    jpql.append("e.")
                            .append(nomeCampo)
                            .append(" = :")
                            .append(nomeParametro);

                    contatore++;
                }
            }

            TypedQuery<T> query = em.createQuery(jpql.toString(), classe);

            for (String nomeCampo : campi.keySet()) {
                String nomeParametro = nomeCampo.replace(".", "_");
                query.setParameter(nomeParametro, campi.get(nomeCampo));
            }

            return query.getResultList();

        } finally {
            em.close();
        }
    }

    /*
     * Cerca il primo oggetto che soddisfa un insieme di condizioni.
     * Restituisce null se non trova nessun risultato.
     */
    public <T> T cercaPrimoPerCampi(Class<T> classe, Map<String, Object> campi) {
        List<T> risultati = cercaPerCampi(classe, campi);

        if (risultati.isEmpty()) {
            return null;
        }

        return risultati.get(0);
    }

    /*
     * Aggiorna un oggetto esistente tramite merge e restituisce l'oggetto aggiornato.
     */
    public <T> T aggiorna(T oggetto) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();

            T oggettoAggiornato = em.merge(oggetto);

            em.getTransaction().commit();
            return oggettoAggiornato;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw e;

        } finally {
            em.close();
        }
    }

    /*
     * Elimina l'oggetto della classe indicata con l'id indicato.
     * Restituisce true se l'oggetto esisteva ed è stato eliminato.
     */
    public <T> boolean elimina(Class<T> classe, Long id) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();

            T oggetto = em.find(classe, id);

            if (oggetto != null) {
                em.remove(oggetto);
                em.getTransaction().commit();
                return true;
            }

            em.getTransaction().commit();
            return false;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            e.printStackTrace();
            return false;

        } finally {
            em.close();
        }
    }
}

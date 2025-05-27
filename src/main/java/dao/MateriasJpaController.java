/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import dto.Clases;
import dto.Materias;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class MateriasJpaController implements Serializable {

    public MateriasJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Materias materias) {
        if (materias.getClasesCollection() == null) {
            materias.setClasesCollection(new ArrayList<Clases>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Clases> attachedClasesCollection = new ArrayList<Clases>();
            for (Clases clasesCollectionClasesToAttach : materias.getClasesCollection()) {
                clasesCollectionClasesToAttach = em.getReference(clasesCollectionClasesToAttach.getClass(), clasesCollectionClasesToAttach.getIdClase());
                attachedClasesCollection.add(clasesCollectionClasesToAttach);
            }
            materias.setClasesCollection(attachedClasesCollection);
            em.persist(materias);
            for (Clases clasesCollectionClases : materias.getClasesCollection()) {
                Materias oldIdMateriaOfClasesCollectionClases = clasesCollectionClases.getIdMateria();
                clasesCollectionClases.setIdMateria(materias);
                clasesCollectionClases = em.merge(clasesCollectionClases);
                if (oldIdMateriaOfClasesCollectionClases != null) {
                    oldIdMateriaOfClasesCollectionClases.getClasesCollection().remove(clasesCollectionClases);
                    oldIdMateriaOfClasesCollectionClases = em.merge(oldIdMateriaOfClasesCollectionClases);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Materias materias) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Materias persistentMaterias = em.find(Materias.class, materias.getIdMateria());
            Collection<Clases> clasesCollectionOld = persistentMaterias.getClasesCollection();
            Collection<Clases> clasesCollectionNew = materias.getClasesCollection();
            List<String> illegalOrphanMessages = null;
            for (Clases clasesCollectionOldClases : clasesCollectionOld) {
                if (!clasesCollectionNew.contains(clasesCollectionOldClases)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Clases " + clasesCollectionOldClases + " since its idMateria field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Clases> attachedClasesCollectionNew = new ArrayList<Clases>();
            for (Clases clasesCollectionNewClasesToAttach : clasesCollectionNew) {
                clasesCollectionNewClasesToAttach = em.getReference(clasesCollectionNewClasesToAttach.getClass(), clasesCollectionNewClasesToAttach.getIdClase());
                attachedClasesCollectionNew.add(clasesCollectionNewClasesToAttach);
            }
            clasesCollectionNew = attachedClasesCollectionNew;
            materias.setClasesCollection(clasesCollectionNew);
            materias = em.merge(materias);
            for (Clases clasesCollectionNewClases : clasesCollectionNew) {
                if (!clasesCollectionOld.contains(clasesCollectionNewClases)) {
                    Materias oldIdMateriaOfClasesCollectionNewClases = clasesCollectionNewClases.getIdMateria();
                    clasesCollectionNewClases.setIdMateria(materias);
                    clasesCollectionNewClases = em.merge(clasesCollectionNewClases);
                    if (oldIdMateriaOfClasesCollectionNewClases != null && !oldIdMateriaOfClasesCollectionNewClases.equals(materias)) {
                        oldIdMateriaOfClasesCollectionNewClases.getClasesCollection().remove(clasesCollectionNewClases);
                        oldIdMateriaOfClasesCollectionNewClases = em.merge(oldIdMateriaOfClasesCollectionNewClases);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = materias.getIdMateria();
                if (findMaterias(id) == null) {
                    throw new NonexistentEntityException("The materias with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Materias materias;
            try {
                materias = em.getReference(Materias.class, id);
                materias.getIdMateria();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The materias with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Clases> clasesCollectionOrphanCheck = materias.getClasesCollection();
            for (Clases clasesCollectionOrphanCheckClases : clasesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Materias (" + materias + ") cannot be destroyed since the Clases " + clasesCollectionOrphanCheckClases + " in its clasesCollection field has a non-nullable idMateria field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(materias);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Materias> findMateriasEntities() {
        return findMateriasEntities(true, -1, -1);
    }

    public List<Materias> findMateriasEntities(int maxResults, int firstResult) {
        return findMateriasEntities(false, maxResults, firstResult);
    }

    private List<Materias> findMateriasEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Materias.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Materias findMaterias(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Materias.class, id);
        } finally {
            em.close();
        }
    }

    public int getMateriasCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Materias> rt = cq.from(Materias.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

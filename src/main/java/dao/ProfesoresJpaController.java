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
import dto.Profesores;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class ProfesoresJpaController implements Serializable {

    public ProfesoresJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Profesores profesores) {
        if (profesores.getClasesCollection() == null) {
            profesores.setClasesCollection(new ArrayList<Clases>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Clases> attachedClasesCollection = new ArrayList<Clases>();
            for (Clases clasesCollectionClasesToAttach : profesores.getClasesCollection()) {
                clasesCollectionClasesToAttach = em.getReference(clasesCollectionClasesToAttach.getClass(), clasesCollectionClasesToAttach.getIdClase());
                attachedClasesCollection.add(clasesCollectionClasesToAttach);
            }
            profesores.setClasesCollection(attachedClasesCollection);
            em.persist(profesores);
            for (Clases clasesCollectionClases : profesores.getClasesCollection()) {
                Profesores oldIdProfesorOfClasesCollectionClases = clasesCollectionClases.getIdProfesor();
                clasesCollectionClases.setIdProfesor(profesores);
                clasesCollectionClases = em.merge(clasesCollectionClases);
                if (oldIdProfesorOfClasesCollectionClases != null) {
                    oldIdProfesorOfClasesCollectionClases.getClasesCollection().remove(clasesCollectionClases);
                    oldIdProfesorOfClasesCollectionClases = em.merge(oldIdProfesorOfClasesCollectionClases);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Profesores profesores) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Profesores persistentProfesores = em.find(Profesores.class, profesores.getIdProfesor());
            Collection<Clases> clasesCollectionOld = persistentProfesores.getClasesCollection();
            Collection<Clases> clasesCollectionNew = profesores.getClasesCollection();
            List<String> illegalOrphanMessages = null;
            for (Clases clasesCollectionOldClases : clasesCollectionOld) {
                if (!clasesCollectionNew.contains(clasesCollectionOldClases)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Clases " + clasesCollectionOldClases + " since its idProfesor field is not nullable.");
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
            profesores.setClasesCollection(clasesCollectionNew);
            profesores = em.merge(profesores);
            for (Clases clasesCollectionNewClases : clasesCollectionNew) {
                if (!clasesCollectionOld.contains(clasesCollectionNewClases)) {
                    Profesores oldIdProfesorOfClasesCollectionNewClases = clasesCollectionNewClases.getIdProfesor();
                    clasesCollectionNewClases.setIdProfesor(profesores);
                    clasesCollectionNewClases = em.merge(clasesCollectionNewClases);
                    if (oldIdProfesorOfClasesCollectionNewClases != null && !oldIdProfesorOfClasesCollectionNewClases.equals(profesores)) {
                        oldIdProfesorOfClasesCollectionNewClases.getClasesCollection().remove(clasesCollectionNewClases);
                        oldIdProfesorOfClasesCollectionNewClases = em.merge(oldIdProfesorOfClasesCollectionNewClases);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = profesores.getIdProfesor();
                if (findProfesores(id) == null) {
                    throw new NonexistentEntityException("The profesores with id " + id + " no longer exists.");
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
            Profesores profesores;
            try {
                profesores = em.getReference(Profesores.class, id);
                profesores.getIdProfesor();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The profesores with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Clases> clasesCollectionOrphanCheck = profesores.getClasesCollection();
            for (Clases clasesCollectionOrphanCheckClases : clasesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Profesores (" + profesores + ") cannot be destroyed since the Clases " + clasesCollectionOrphanCheckClases + " in its clasesCollection field has a non-nullable idProfesor field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(profesores);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Profesores> findProfesoresEntities() {
        return findProfesoresEntities(true, -1, -1);
    }

    public List<Profesores> findProfesoresEntities(int maxResults, int firstResult) {
        return findProfesoresEntities(false, maxResults, firstResult);
    }

    private List<Profesores> findProfesoresEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Profesores.class));
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

    public Profesores findProfesores(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Profesores.class, id);
        } finally {
            em.close();
        }
    }

    public int getProfesoresCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Profesores> rt = cq.from(Profesores.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

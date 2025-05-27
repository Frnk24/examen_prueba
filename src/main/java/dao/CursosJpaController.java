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
import dto.Cursos;
import java.util.ArrayList;
import java.util.Collection;
import dto.Inscripciones;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class CursosJpaController implements Serializable {

    public CursosJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Cursos cursos) {
        if (cursos.getClasesCollection() == null) {
            cursos.setClasesCollection(new ArrayList<Clases>());
        }
        if (cursos.getInscripcionesCollection() == null) {
            cursos.setInscripcionesCollection(new ArrayList<Inscripciones>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Clases> attachedClasesCollection = new ArrayList<Clases>();
            for (Clases clasesCollectionClasesToAttach : cursos.getClasesCollection()) {
                clasesCollectionClasesToAttach = em.getReference(clasesCollectionClasesToAttach.getClass(), clasesCollectionClasesToAttach.getIdClase());
                attachedClasesCollection.add(clasesCollectionClasesToAttach);
            }
            cursos.setClasesCollection(attachedClasesCollection);
            Collection<Inscripciones> attachedInscripcionesCollection = new ArrayList<Inscripciones>();
            for (Inscripciones inscripcionesCollectionInscripcionesToAttach : cursos.getInscripcionesCollection()) {
                inscripcionesCollectionInscripcionesToAttach = em.getReference(inscripcionesCollectionInscripcionesToAttach.getClass(), inscripcionesCollectionInscripcionesToAttach.getIdInscripcion());
                attachedInscripcionesCollection.add(inscripcionesCollectionInscripcionesToAttach);
            }
            cursos.setInscripcionesCollection(attachedInscripcionesCollection);
            em.persist(cursos);
            for (Clases clasesCollectionClases : cursos.getClasesCollection()) {
                Cursos oldIdCursoOfClasesCollectionClases = clasesCollectionClases.getIdCurso();
                clasesCollectionClases.setIdCurso(cursos);
                clasesCollectionClases = em.merge(clasesCollectionClases);
                if (oldIdCursoOfClasesCollectionClases != null) {
                    oldIdCursoOfClasesCollectionClases.getClasesCollection().remove(clasesCollectionClases);
                    oldIdCursoOfClasesCollectionClases = em.merge(oldIdCursoOfClasesCollectionClases);
                }
            }
            for (Inscripciones inscripcionesCollectionInscripciones : cursos.getInscripcionesCollection()) {
                Cursos oldIdCursoOfInscripcionesCollectionInscripciones = inscripcionesCollectionInscripciones.getIdCurso();
                inscripcionesCollectionInscripciones.setIdCurso(cursos);
                inscripcionesCollectionInscripciones = em.merge(inscripcionesCollectionInscripciones);
                if (oldIdCursoOfInscripcionesCollectionInscripciones != null) {
                    oldIdCursoOfInscripcionesCollectionInscripciones.getInscripcionesCollection().remove(inscripcionesCollectionInscripciones);
                    oldIdCursoOfInscripcionesCollectionInscripciones = em.merge(oldIdCursoOfInscripcionesCollectionInscripciones);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Cursos cursos) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cursos persistentCursos = em.find(Cursos.class, cursos.getIdCurso());
            Collection<Clases> clasesCollectionOld = persistentCursos.getClasesCollection();
            Collection<Clases> clasesCollectionNew = cursos.getClasesCollection();
            Collection<Inscripciones> inscripcionesCollectionOld = persistentCursos.getInscripcionesCollection();
            Collection<Inscripciones> inscripcionesCollectionNew = cursos.getInscripcionesCollection();
            List<String> illegalOrphanMessages = null;
            for (Clases clasesCollectionOldClases : clasesCollectionOld) {
                if (!clasesCollectionNew.contains(clasesCollectionOldClases)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Clases " + clasesCollectionOldClases + " since its idCurso field is not nullable.");
                }
            }
            for (Inscripciones inscripcionesCollectionOldInscripciones : inscripcionesCollectionOld) {
                if (!inscripcionesCollectionNew.contains(inscripcionesCollectionOldInscripciones)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Inscripciones " + inscripcionesCollectionOldInscripciones + " since its idCurso field is not nullable.");
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
            cursos.setClasesCollection(clasesCollectionNew);
            Collection<Inscripciones> attachedInscripcionesCollectionNew = new ArrayList<Inscripciones>();
            for (Inscripciones inscripcionesCollectionNewInscripcionesToAttach : inscripcionesCollectionNew) {
                inscripcionesCollectionNewInscripcionesToAttach = em.getReference(inscripcionesCollectionNewInscripcionesToAttach.getClass(), inscripcionesCollectionNewInscripcionesToAttach.getIdInscripcion());
                attachedInscripcionesCollectionNew.add(inscripcionesCollectionNewInscripcionesToAttach);
            }
            inscripcionesCollectionNew = attachedInscripcionesCollectionNew;
            cursos.setInscripcionesCollection(inscripcionesCollectionNew);
            cursos = em.merge(cursos);
            for (Clases clasesCollectionNewClases : clasesCollectionNew) {
                if (!clasesCollectionOld.contains(clasesCollectionNewClases)) {
                    Cursos oldIdCursoOfClasesCollectionNewClases = clasesCollectionNewClases.getIdCurso();
                    clasesCollectionNewClases.setIdCurso(cursos);
                    clasesCollectionNewClases = em.merge(clasesCollectionNewClases);
                    if (oldIdCursoOfClasesCollectionNewClases != null && !oldIdCursoOfClasesCollectionNewClases.equals(cursos)) {
                        oldIdCursoOfClasesCollectionNewClases.getClasesCollection().remove(clasesCollectionNewClases);
                        oldIdCursoOfClasesCollectionNewClases = em.merge(oldIdCursoOfClasesCollectionNewClases);
                    }
                }
            }
            for (Inscripciones inscripcionesCollectionNewInscripciones : inscripcionesCollectionNew) {
                if (!inscripcionesCollectionOld.contains(inscripcionesCollectionNewInscripciones)) {
                    Cursos oldIdCursoOfInscripcionesCollectionNewInscripciones = inscripcionesCollectionNewInscripciones.getIdCurso();
                    inscripcionesCollectionNewInscripciones.setIdCurso(cursos);
                    inscripcionesCollectionNewInscripciones = em.merge(inscripcionesCollectionNewInscripciones);
                    if (oldIdCursoOfInscripcionesCollectionNewInscripciones != null && !oldIdCursoOfInscripcionesCollectionNewInscripciones.equals(cursos)) {
                        oldIdCursoOfInscripcionesCollectionNewInscripciones.getInscripcionesCollection().remove(inscripcionesCollectionNewInscripciones);
                        oldIdCursoOfInscripcionesCollectionNewInscripciones = em.merge(oldIdCursoOfInscripcionesCollectionNewInscripciones);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = cursos.getIdCurso();
                if (findCursos(id) == null) {
                    throw new NonexistentEntityException("The cursos with id " + id + " no longer exists.");
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
            Cursos cursos;
            try {
                cursos = em.getReference(Cursos.class, id);
                cursos.getIdCurso();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The cursos with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Clases> clasesCollectionOrphanCheck = cursos.getClasesCollection();
            for (Clases clasesCollectionOrphanCheckClases : clasesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Cursos (" + cursos + ") cannot be destroyed since the Clases " + clasesCollectionOrphanCheckClases + " in its clasesCollection field has a non-nullable idCurso field.");
            }
            Collection<Inscripciones> inscripcionesCollectionOrphanCheck = cursos.getInscripcionesCollection();
            for (Inscripciones inscripcionesCollectionOrphanCheckInscripciones : inscripcionesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Cursos (" + cursos + ") cannot be destroyed since the Inscripciones " + inscripcionesCollectionOrphanCheckInscripciones + " in its inscripcionesCollection field has a non-nullable idCurso field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(cursos);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Cursos> findCursosEntities() {
        return findCursosEntities(true, -1, -1);
    }

    public List<Cursos> findCursosEntities(int maxResults, int firstResult) {
        return findCursosEntities(false, maxResults, firstResult);
    }

    private List<Cursos> findCursosEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Cursos.class));
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

    public Cursos findCursos(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cursos.class, id);
        } finally {
            em.close();
        }
    }

    public int getCursosCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Cursos> rt = cq.from(Cursos.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import dto.Estudiantes;
import dto.Cursos;
import dto.Inscripciones;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class InscripcionesJpaController implements Serializable {

    public InscripcionesJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Inscripciones inscripciones) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estudiantes idEstudiante = inscripciones.getIdEstudiante();
            if (idEstudiante != null) {
                idEstudiante = em.getReference(idEstudiante.getClass(), idEstudiante.getIdEstudiante());
                inscripciones.setIdEstudiante(idEstudiante);
            }
            Cursos idCurso = inscripciones.getIdCurso();
            if (idCurso != null) {
                idCurso = em.getReference(idCurso.getClass(), idCurso.getIdCurso());
                inscripciones.setIdCurso(idCurso);
            }
            em.persist(inscripciones);
            if (idEstudiante != null) {
                idEstudiante.getInscripcionesCollection().add(inscripciones);
                idEstudiante = em.merge(idEstudiante);
            }
            if (idCurso != null) {
                idCurso.getInscripcionesCollection().add(inscripciones);
                idCurso = em.merge(idCurso);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Inscripciones inscripciones) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Inscripciones persistentInscripciones = em.find(Inscripciones.class, inscripciones.getIdInscripcion());
            Estudiantes idEstudianteOld = persistentInscripciones.getIdEstudiante();
            Estudiantes idEstudianteNew = inscripciones.getIdEstudiante();
            Cursos idCursoOld = persistentInscripciones.getIdCurso();
            Cursos idCursoNew = inscripciones.getIdCurso();
            if (idEstudianteNew != null) {
                idEstudianteNew = em.getReference(idEstudianteNew.getClass(), idEstudianteNew.getIdEstudiante());
                inscripciones.setIdEstudiante(idEstudianteNew);
            }
            if (idCursoNew != null) {
                idCursoNew = em.getReference(idCursoNew.getClass(), idCursoNew.getIdCurso());
                inscripciones.setIdCurso(idCursoNew);
            }
            inscripciones = em.merge(inscripciones);
            if (idEstudianteOld != null && !idEstudianteOld.equals(idEstudianteNew)) {
                idEstudianteOld.getInscripcionesCollection().remove(inscripciones);
                idEstudianteOld = em.merge(idEstudianteOld);
            }
            if (idEstudianteNew != null && !idEstudianteNew.equals(idEstudianteOld)) {
                idEstudianteNew.getInscripcionesCollection().add(inscripciones);
                idEstudianteNew = em.merge(idEstudianteNew);
            }
            if (idCursoOld != null && !idCursoOld.equals(idCursoNew)) {
                idCursoOld.getInscripcionesCollection().remove(inscripciones);
                idCursoOld = em.merge(idCursoOld);
            }
            if (idCursoNew != null && !idCursoNew.equals(idCursoOld)) {
                idCursoNew.getInscripcionesCollection().add(inscripciones);
                idCursoNew = em.merge(idCursoNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = inscripciones.getIdInscripcion();
                if (findInscripciones(id) == null) {
                    throw new NonexistentEntityException("The inscripciones with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Inscripciones inscripciones;
            try {
                inscripciones = em.getReference(Inscripciones.class, id);
                inscripciones.getIdInscripcion();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The inscripciones with id " + id + " no longer exists.", enfe);
            }
            Estudiantes idEstudiante = inscripciones.getIdEstudiante();
            if (idEstudiante != null) {
                idEstudiante.getInscripcionesCollection().remove(inscripciones);
                idEstudiante = em.merge(idEstudiante);
            }
            Cursos idCurso = inscripciones.getIdCurso();
            if (idCurso != null) {
                idCurso.getInscripcionesCollection().remove(inscripciones);
                idCurso = em.merge(idCurso);
            }
            em.remove(inscripciones);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Inscripciones> findInscripcionesEntities() {
        return findInscripcionesEntities(true, -1, -1);
    }

    public List<Inscripciones> findInscripcionesEntities(int maxResults, int firstResult) {
        return findInscripcionesEntities(false, maxResults, firstResult);
    }

    private List<Inscripciones> findInscripcionesEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Inscripciones.class));
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

    public Inscripciones findInscripciones(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Inscripciones.class, id);
        } finally {
            em.close();
        }
    }

    public int getInscripcionesCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Inscripciones> rt = cq.from(Inscripciones.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

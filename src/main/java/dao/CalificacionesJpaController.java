/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import dto.Calificaciones;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import dto.Estudiantes;
import dto.Clases;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class CalificacionesJpaController implements Serializable {

    public CalificacionesJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Calificaciones calificaciones) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estudiantes idEstudiante = calificaciones.getIdEstudiante();
            if (idEstudiante != null) {
                idEstudiante = em.getReference(idEstudiante.getClass(), idEstudiante.getIdEstudiante());
                calificaciones.setIdEstudiante(idEstudiante);
            }
            Clases idClase = calificaciones.getIdClase();
            if (idClase != null) {
                idClase = em.getReference(idClase.getClass(), idClase.getIdClase());
                calificaciones.setIdClase(idClase);
            }
            em.persist(calificaciones);
            if (idEstudiante != null) {
                idEstudiante.getCalificacionesCollection().add(calificaciones);
                idEstudiante = em.merge(idEstudiante);
            }
            if (idClase != null) {
                idClase.getCalificacionesCollection().add(calificaciones);
                idClase = em.merge(idClase);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Calificaciones calificaciones) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Calificaciones persistentCalificaciones = em.find(Calificaciones.class, calificaciones.getIdCalificacion());
            Estudiantes idEstudianteOld = persistentCalificaciones.getIdEstudiante();
            Estudiantes idEstudianteNew = calificaciones.getIdEstudiante();
            Clases idClaseOld = persistentCalificaciones.getIdClase();
            Clases idClaseNew = calificaciones.getIdClase();
            if (idEstudianteNew != null) {
                idEstudianteNew = em.getReference(idEstudianteNew.getClass(), idEstudianteNew.getIdEstudiante());
                calificaciones.setIdEstudiante(idEstudianteNew);
            }
            if (idClaseNew != null) {
                idClaseNew = em.getReference(idClaseNew.getClass(), idClaseNew.getIdClase());
                calificaciones.setIdClase(idClaseNew);
            }
            calificaciones = em.merge(calificaciones);
            if (idEstudianteOld != null && !idEstudianteOld.equals(idEstudianteNew)) {
                idEstudianteOld.getCalificacionesCollection().remove(calificaciones);
                idEstudianteOld = em.merge(idEstudianteOld);
            }
            if (idEstudianteNew != null && !idEstudianteNew.equals(idEstudianteOld)) {
                idEstudianteNew.getCalificacionesCollection().add(calificaciones);
                idEstudianteNew = em.merge(idEstudianteNew);
            }
            if (idClaseOld != null && !idClaseOld.equals(idClaseNew)) {
                idClaseOld.getCalificacionesCollection().remove(calificaciones);
                idClaseOld = em.merge(idClaseOld);
            }
            if (idClaseNew != null && !idClaseNew.equals(idClaseOld)) {
                idClaseNew.getCalificacionesCollection().add(calificaciones);
                idClaseNew = em.merge(idClaseNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = calificaciones.getIdCalificacion();
                if (findCalificaciones(id) == null) {
                    throw new NonexistentEntityException("The calificaciones with id " + id + " no longer exists.");
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
            Calificaciones calificaciones;
            try {
                calificaciones = em.getReference(Calificaciones.class, id);
                calificaciones.getIdCalificacion();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The calificaciones with id " + id + " no longer exists.", enfe);
            }
            Estudiantes idEstudiante = calificaciones.getIdEstudiante();
            if (idEstudiante != null) {
                idEstudiante.getCalificacionesCollection().remove(calificaciones);
                idEstudiante = em.merge(idEstudiante);
            }
            Clases idClase = calificaciones.getIdClase();
            if (idClase != null) {
                idClase.getCalificacionesCollection().remove(calificaciones);
                idClase = em.merge(idClase);
            }
            em.remove(calificaciones);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Calificaciones> findCalificacionesEntities() {
        return findCalificacionesEntities(true, -1, -1);
    }

    public List<Calificaciones> findCalificacionesEntities(int maxResults, int firstResult) {
        return findCalificacionesEntities(false, maxResults, firstResult);
    }

    private List<Calificaciones> findCalificacionesEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Calificaciones.class));
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

    public Calificaciones findCalificaciones(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Calificaciones.class, id);
        } finally {
            em.close();
        }
    }

    public int getCalificacionesCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Calificaciones> rt = cq.from(Calificaciones.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

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
import dto.Materias;
import dto.Profesores;
import dto.Cursos;
import dto.Asistencia;
import java.util.ArrayList;
import java.util.Collection;
import dto.Calificaciones;
import dto.Clases;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class ClasesJpaController implements Serializable {

    public ClasesJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Clases clases) {
        if (clases.getAsistenciaCollection() == null) {
            clases.setAsistenciaCollection(new ArrayList<Asistencia>());
        }
        if (clases.getCalificacionesCollection() == null) {
            clases.setCalificacionesCollection(new ArrayList<Calificaciones>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Materias idMateria = clases.getIdMateria();
            if (idMateria != null) {
                idMateria = em.getReference(idMateria.getClass(), idMateria.getIdMateria());
                clases.setIdMateria(idMateria);
            }
            Profesores idProfesor = clases.getIdProfesor();
            if (idProfesor != null) {
                idProfesor = em.getReference(idProfesor.getClass(), idProfesor.getIdProfesor());
                clases.setIdProfesor(idProfesor);
            }
            Cursos idCurso = clases.getIdCurso();
            if (idCurso != null) {
                idCurso = em.getReference(idCurso.getClass(), idCurso.getIdCurso());
                clases.setIdCurso(idCurso);
            }
            Collection<Asistencia> attachedAsistenciaCollection = new ArrayList<Asistencia>();
            for (Asistencia asistenciaCollectionAsistenciaToAttach : clases.getAsistenciaCollection()) {
                asistenciaCollectionAsistenciaToAttach = em.getReference(asistenciaCollectionAsistenciaToAttach.getClass(), asistenciaCollectionAsistenciaToAttach.getIdAsistencia());
                attachedAsistenciaCollection.add(asistenciaCollectionAsistenciaToAttach);
            }
            clases.setAsistenciaCollection(attachedAsistenciaCollection);
            Collection<Calificaciones> attachedCalificacionesCollection = new ArrayList<Calificaciones>();
            for (Calificaciones calificacionesCollectionCalificacionesToAttach : clases.getCalificacionesCollection()) {
                calificacionesCollectionCalificacionesToAttach = em.getReference(calificacionesCollectionCalificacionesToAttach.getClass(), calificacionesCollectionCalificacionesToAttach.getIdCalificacion());
                attachedCalificacionesCollection.add(calificacionesCollectionCalificacionesToAttach);
            }
            clases.setCalificacionesCollection(attachedCalificacionesCollection);
            em.persist(clases);
            if (idMateria != null) {
                idMateria.getClasesCollection().add(clases);
                idMateria = em.merge(idMateria);
            }
            if (idProfesor != null) {
                idProfesor.getClasesCollection().add(clases);
                idProfesor = em.merge(idProfesor);
            }
            if (idCurso != null) {
                idCurso.getClasesCollection().add(clases);
                idCurso = em.merge(idCurso);
            }
            for (Asistencia asistenciaCollectionAsistencia : clases.getAsistenciaCollection()) {
                Clases oldIdClaseOfAsistenciaCollectionAsistencia = asistenciaCollectionAsistencia.getIdClase();
                asistenciaCollectionAsistencia.setIdClase(clases);
                asistenciaCollectionAsistencia = em.merge(asistenciaCollectionAsistencia);
                if (oldIdClaseOfAsistenciaCollectionAsistencia != null) {
                    oldIdClaseOfAsistenciaCollectionAsistencia.getAsistenciaCollection().remove(asistenciaCollectionAsistencia);
                    oldIdClaseOfAsistenciaCollectionAsistencia = em.merge(oldIdClaseOfAsistenciaCollectionAsistencia);
                }
            }
            for (Calificaciones calificacionesCollectionCalificaciones : clases.getCalificacionesCollection()) {
                Clases oldIdClaseOfCalificacionesCollectionCalificaciones = calificacionesCollectionCalificaciones.getIdClase();
                calificacionesCollectionCalificaciones.setIdClase(clases);
                calificacionesCollectionCalificaciones = em.merge(calificacionesCollectionCalificaciones);
                if (oldIdClaseOfCalificacionesCollectionCalificaciones != null) {
                    oldIdClaseOfCalificacionesCollectionCalificaciones.getCalificacionesCollection().remove(calificacionesCollectionCalificaciones);
                    oldIdClaseOfCalificacionesCollectionCalificaciones = em.merge(oldIdClaseOfCalificacionesCollectionCalificaciones);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Clases clases) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Clases persistentClases = em.find(Clases.class, clases.getIdClase());
            Materias idMateriaOld = persistentClases.getIdMateria();
            Materias idMateriaNew = clases.getIdMateria();
            Profesores idProfesorOld = persistentClases.getIdProfesor();
            Profesores idProfesorNew = clases.getIdProfesor();
            Cursos idCursoOld = persistentClases.getIdCurso();
            Cursos idCursoNew = clases.getIdCurso();
            Collection<Asistencia> asistenciaCollectionOld = persistentClases.getAsistenciaCollection();
            Collection<Asistencia> asistenciaCollectionNew = clases.getAsistenciaCollection();
            Collection<Calificaciones> calificacionesCollectionOld = persistentClases.getCalificacionesCollection();
            Collection<Calificaciones> calificacionesCollectionNew = clases.getCalificacionesCollection();
            List<String> illegalOrphanMessages = null;
            for (Asistencia asistenciaCollectionOldAsistencia : asistenciaCollectionOld) {
                if (!asistenciaCollectionNew.contains(asistenciaCollectionOldAsistencia)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Asistencia " + asistenciaCollectionOldAsistencia + " since its idClase field is not nullable.");
                }
            }
            for (Calificaciones calificacionesCollectionOldCalificaciones : calificacionesCollectionOld) {
                if (!calificacionesCollectionNew.contains(calificacionesCollectionOldCalificaciones)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Calificaciones " + calificacionesCollectionOldCalificaciones + " since its idClase field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (idMateriaNew != null) {
                idMateriaNew = em.getReference(idMateriaNew.getClass(), idMateriaNew.getIdMateria());
                clases.setIdMateria(idMateriaNew);
            }
            if (idProfesorNew != null) {
                idProfesorNew = em.getReference(idProfesorNew.getClass(), idProfesorNew.getIdProfesor());
                clases.setIdProfesor(idProfesorNew);
            }
            if (idCursoNew != null) {
                idCursoNew = em.getReference(idCursoNew.getClass(), idCursoNew.getIdCurso());
                clases.setIdCurso(idCursoNew);
            }
            Collection<Asistencia> attachedAsistenciaCollectionNew = new ArrayList<Asistencia>();
            for (Asistencia asistenciaCollectionNewAsistenciaToAttach : asistenciaCollectionNew) {
                asistenciaCollectionNewAsistenciaToAttach = em.getReference(asistenciaCollectionNewAsistenciaToAttach.getClass(), asistenciaCollectionNewAsistenciaToAttach.getIdAsistencia());
                attachedAsistenciaCollectionNew.add(asistenciaCollectionNewAsistenciaToAttach);
            }
            asistenciaCollectionNew = attachedAsistenciaCollectionNew;
            clases.setAsistenciaCollection(asistenciaCollectionNew);
            Collection<Calificaciones> attachedCalificacionesCollectionNew = new ArrayList<Calificaciones>();
            for (Calificaciones calificacionesCollectionNewCalificacionesToAttach : calificacionesCollectionNew) {
                calificacionesCollectionNewCalificacionesToAttach = em.getReference(calificacionesCollectionNewCalificacionesToAttach.getClass(), calificacionesCollectionNewCalificacionesToAttach.getIdCalificacion());
                attachedCalificacionesCollectionNew.add(calificacionesCollectionNewCalificacionesToAttach);
            }
            calificacionesCollectionNew = attachedCalificacionesCollectionNew;
            clases.setCalificacionesCollection(calificacionesCollectionNew);
            clases = em.merge(clases);
            if (idMateriaOld != null && !idMateriaOld.equals(idMateriaNew)) {
                idMateriaOld.getClasesCollection().remove(clases);
                idMateriaOld = em.merge(idMateriaOld);
            }
            if (idMateriaNew != null && !idMateriaNew.equals(idMateriaOld)) {
                idMateriaNew.getClasesCollection().add(clases);
                idMateriaNew = em.merge(idMateriaNew);
            }
            if (idProfesorOld != null && !idProfesorOld.equals(idProfesorNew)) {
                idProfesorOld.getClasesCollection().remove(clases);
                idProfesorOld = em.merge(idProfesorOld);
            }
            if (idProfesorNew != null && !idProfesorNew.equals(idProfesorOld)) {
                idProfesorNew.getClasesCollection().add(clases);
                idProfesorNew = em.merge(idProfesorNew);
            }
            if (idCursoOld != null && !idCursoOld.equals(idCursoNew)) {
                idCursoOld.getClasesCollection().remove(clases);
                idCursoOld = em.merge(idCursoOld);
            }
            if (idCursoNew != null && !idCursoNew.equals(idCursoOld)) {
                idCursoNew.getClasesCollection().add(clases);
                idCursoNew = em.merge(idCursoNew);
            }
            for (Asistencia asistenciaCollectionNewAsistencia : asistenciaCollectionNew) {
                if (!asistenciaCollectionOld.contains(asistenciaCollectionNewAsistencia)) {
                    Clases oldIdClaseOfAsistenciaCollectionNewAsistencia = asistenciaCollectionNewAsistencia.getIdClase();
                    asistenciaCollectionNewAsistencia.setIdClase(clases);
                    asistenciaCollectionNewAsistencia = em.merge(asistenciaCollectionNewAsistencia);
                    if (oldIdClaseOfAsistenciaCollectionNewAsistencia != null && !oldIdClaseOfAsistenciaCollectionNewAsistencia.equals(clases)) {
                        oldIdClaseOfAsistenciaCollectionNewAsistencia.getAsistenciaCollection().remove(asistenciaCollectionNewAsistencia);
                        oldIdClaseOfAsistenciaCollectionNewAsistencia = em.merge(oldIdClaseOfAsistenciaCollectionNewAsistencia);
                    }
                }
            }
            for (Calificaciones calificacionesCollectionNewCalificaciones : calificacionesCollectionNew) {
                if (!calificacionesCollectionOld.contains(calificacionesCollectionNewCalificaciones)) {
                    Clases oldIdClaseOfCalificacionesCollectionNewCalificaciones = calificacionesCollectionNewCalificaciones.getIdClase();
                    calificacionesCollectionNewCalificaciones.setIdClase(clases);
                    calificacionesCollectionNewCalificaciones = em.merge(calificacionesCollectionNewCalificaciones);
                    if (oldIdClaseOfCalificacionesCollectionNewCalificaciones != null && !oldIdClaseOfCalificacionesCollectionNewCalificaciones.equals(clases)) {
                        oldIdClaseOfCalificacionesCollectionNewCalificaciones.getCalificacionesCollection().remove(calificacionesCollectionNewCalificaciones);
                        oldIdClaseOfCalificacionesCollectionNewCalificaciones = em.merge(oldIdClaseOfCalificacionesCollectionNewCalificaciones);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = clases.getIdClase();
                if (findClases(id) == null) {
                    throw new NonexistentEntityException("The clases with id " + id + " no longer exists.");
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
            Clases clases;
            try {
                clases = em.getReference(Clases.class, id);
                clases.getIdClase();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The clases with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Asistencia> asistenciaCollectionOrphanCheck = clases.getAsistenciaCollection();
            for (Asistencia asistenciaCollectionOrphanCheckAsistencia : asistenciaCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Clases (" + clases + ") cannot be destroyed since the Asistencia " + asistenciaCollectionOrphanCheckAsistencia + " in its asistenciaCollection field has a non-nullable idClase field.");
            }
            Collection<Calificaciones> calificacionesCollectionOrphanCheck = clases.getCalificacionesCollection();
            for (Calificaciones calificacionesCollectionOrphanCheckCalificaciones : calificacionesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Clases (" + clases + ") cannot be destroyed since the Calificaciones " + calificacionesCollectionOrphanCheckCalificaciones + " in its calificacionesCollection field has a non-nullable idClase field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Materias idMateria = clases.getIdMateria();
            if (idMateria != null) {
                idMateria.getClasesCollection().remove(clases);
                idMateria = em.merge(idMateria);
            }
            Profesores idProfesor = clases.getIdProfesor();
            if (idProfesor != null) {
                idProfesor.getClasesCollection().remove(clases);
                idProfesor = em.merge(idProfesor);
            }
            Cursos idCurso = clases.getIdCurso();
            if (idCurso != null) {
                idCurso.getClasesCollection().remove(clases);
                idCurso = em.merge(idCurso);
            }
            em.remove(clases);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Clases> findClasesEntities() {
        return findClasesEntities(true, -1, -1);
    }

    public List<Clases> findClasesEntities(int maxResults, int firstResult) {
        return findClasesEntities(false, maxResults, firstResult);
    }

    private List<Clases> findClasesEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Clases.class));
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

    public Clases findClases(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Clases.class, id);
        } finally {
            em.close();
        }
    }

    public int getClasesCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Clases> rt = cq.from(Clases.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

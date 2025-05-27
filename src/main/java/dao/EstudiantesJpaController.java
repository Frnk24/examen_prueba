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
import dto.Asistencia;
import java.util.ArrayList;
import java.util.Collection;
import dto.Calificaciones;
import dto.Estudiantes;
import dto.Inscripciones;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author frank
 */
public class EstudiantesJpaController implements Serializable {

    public EstudiantesJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Estudiantes estudiantes) {
        if (estudiantes.getAsistenciaCollection() == null) {
            estudiantes.setAsistenciaCollection(new ArrayList<Asistencia>());
        }
        if (estudiantes.getCalificacionesCollection() == null) {
            estudiantes.setCalificacionesCollection(new ArrayList<Calificaciones>());
        }
        if (estudiantes.getInscripcionesCollection() == null) {
            estudiantes.setInscripcionesCollection(new ArrayList<Inscripciones>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Asistencia> attachedAsistenciaCollection = new ArrayList<Asistencia>();
            for (Asistencia asistenciaCollectionAsistenciaToAttach : estudiantes.getAsistenciaCollection()) {
                asistenciaCollectionAsistenciaToAttach = em.getReference(asistenciaCollectionAsistenciaToAttach.getClass(), asistenciaCollectionAsistenciaToAttach.getIdAsistencia());
                attachedAsistenciaCollection.add(asistenciaCollectionAsistenciaToAttach);
            }
            estudiantes.setAsistenciaCollection(attachedAsistenciaCollection);
            Collection<Calificaciones> attachedCalificacionesCollection = new ArrayList<Calificaciones>();
            for (Calificaciones calificacionesCollectionCalificacionesToAttach : estudiantes.getCalificacionesCollection()) {
                calificacionesCollectionCalificacionesToAttach = em.getReference(calificacionesCollectionCalificacionesToAttach.getClass(), calificacionesCollectionCalificacionesToAttach.getIdCalificacion());
                attachedCalificacionesCollection.add(calificacionesCollectionCalificacionesToAttach);
            }
            estudiantes.setCalificacionesCollection(attachedCalificacionesCollection);
            Collection<Inscripciones> attachedInscripcionesCollection = new ArrayList<Inscripciones>();
            for (Inscripciones inscripcionesCollectionInscripcionesToAttach : estudiantes.getInscripcionesCollection()) {
                inscripcionesCollectionInscripcionesToAttach = em.getReference(inscripcionesCollectionInscripcionesToAttach.getClass(), inscripcionesCollectionInscripcionesToAttach.getIdInscripcion());
                attachedInscripcionesCollection.add(inscripcionesCollectionInscripcionesToAttach);
            }
            estudiantes.setInscripcionesCollection(attachedInscripcionesCollection);
            em.persist(estudiantes);
            for (Asistencia asistenciaCollectionAsistencia : estudiantes.getAsistenciaCollection()) {
                Estudiantes oldIdEstudianteOfAsistenciaCollectionAsistencia = asistenciaCollectionAsistencia.getIdEstudiante();
                asistenciaCollectionAsistencia.setIdEstudiante(estudiantes);
                asistenciaCollectionAsistencia = em.merge(asistenciaCollectionAsistencia);
                if (oldIdEstudianteOfAsistenciaCollectionAsistencia != null) {
                    oldIdEstudianteOfAsistenciaCollectionAsistencia.getAsistenciaCollection().remove(asistenciaCollectionAsistencia);
                    oldIdEstudianteOfAsistenciaCollectionAsistencia = em.merge(oldIdEstudianteOfAsistenciaCollectionAsistencia);
                }
            }
            for (Calificaciones calificacionesCollectionCalificaciones : estudiantes.getCalificacionesCollection()) {
                Estudiantes oldIdEstudianteOfCalificacionesCollectionCalificaciones = calificacionesCollectionCalificaciones.getIdEstudiante();
                calificacionesCollectionCalificaciones.setIdEstudiante(estudiantes);
                calificacionesCollectionCalificaciones = em.merge(calificacionesCollectionCalificaciones);
                if (oldIdEstudianteOfCalificacionesCollectionCalificaciones != null) {
                    oldIdEstudianteOfCalificacionesCollectionCalificaciones.getCalificacionesCollection().remove(calificacionesCollectionCalificaciones);
                    oldIdEstudianteOfCalificacionesCollectionCalificaciones = em.merge(oldIdEstudianteOfCalificacionesCollectionCalificaciones);
                }
            }
            for (Inscripciones inscripcionesCollectionInscripciones : estudiantes.getInscripcionesCollection()) {
                Estudiantes oldIdEstudianteOfInscripcionesCollectionInscripciones = inscripcionesCollectionInscripciones.getIdEstudiante();
                inscripcionesCollectionInscripciones.setIdEstudiante(estudiantes);
                inscripcionesCollectionInscripciones = em.merge(inscripcionesCollectionInscripciones);
                if (oldIdEstudianteOfInscripcionesCollectionInscripciones != null) {
                    oldIdEstudianteOfInscripcionesCollectionInscripciones.getInscripcionesCollection().remove(inscripcionesCollectionInscripciones);
                    oldIdEstudianteOfInscripcionesCollectionInscripciones = em.merge(oldIdEstudianteOfInscripcionesCollectionInscripciones);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Estudiantes estudiantes) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estudiantes persistentEstudiantes = em.find(Estudiantes.class, estudiantes.getIdEstudiante());
            Collection<Asistencia> asistenciaCollectionOld = persistentEstudiantes.getAsistenciaCollection();
            Collection<Asistencia> asistenciaCollectionNew = estudiantes.getAsistenciaCollection();
            Collection<Calificaciones> calificacionesCollectionOld = persistentEstudiantes.getCalificacionesCollection();
            Collection<Calificaciones> calificacionesCollectionNew = estudiantes.getCalificacionesCollection();
            Collection<Inscripciones> inscripcionesCollectionOld = persistentEstudiantes.getInscripcionesCollection();
            Collection<Inscripciones> inscripcionesCollectionNew = estudiantes.getInscripcionesCollection();
            List<String> illegalOrphanMessages = null;
            for (Asistencia asistenciaCollectionOldAsistencia : asistenciaCollectionOld) {
                if (!asistenciaCollectionNew.contains(asistenciaCollectionOldAsistencia)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Asistencia " + asistenciaCollectionOldAsistencia + " since its idEstudiante field is not nullable.");
                }
            }
            for (Calificaciones calificacionesCollectionOldCalificaciones : calificacionesCollectionOld) {
                if (!calificacionesCollectionNew.contains(calificacionesCollectionOldCalificaciones)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Calificaciones " + calificacionesCollectionOldCalificaciones + " since its idEstudiante field is not nullable.");
                }
            }
            for (Inscripciones inscripcionesCollectionOldInscripciones : inscripcionesCollectionOld) {
                if (!inscripcionesCollectionNew.contains(inscripcionesCollectionOldInscripciones)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Inscripciones " + inscripcionesCollectionOldInscripciones + " since its idEstudiante field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Asistencia> attachedAsistenciaCollectionNew = new ArrayList<Asistencia>();
            for (Asistencia asistenciaCollectionNewAsistenciaToAttach : asistenciaCollectionNew) {
                asistenciaCollectionNewAsistenciaToAttach = em.getReference(asistenciaCollectionNewAsistenciaToAttach.getClass(), asistenciaCollectionNewAsistenciaToAttach.getIdAsistencia());
                attachedAsistenciaCollectionNew.add(asistenciaCollectionNewAsistenciaToAttach);
            }
            asistenciaCollectionNew = attachedAsistenciaCollectionNew;
            estudiantes.setAsistenciaCollection(asistenciaCollectionNew);
            Collection<Calificaciones> attachedCalificacionesCollectionNew = new ArrayList<Calificaciones>();
            for (Calificaciones calificacionesCollectionNewCalificacionesToAttach : calificacionesCollectionNew) {
                calificacionesCollectionNewCalificacionesToAttach = em.getReference(calificacionesCollectionNewCalificacionesToAttach.getClass(), calificacionesCollectionNewCalificacionesToAttach.getIdCalificacion());
                attachedCalificacionesCollectionNew.add(calificacionesCollectionNewCalificacionesToAttach);
            }
            calificacionesCollectionNew = attachedCalificacionesCollectionNew;
            estudiantes.setCalificacionesCollection(calificacionesCollectionNew);
            Collection<Inscripciones> attachedInscripcionesCollectionNew = new ArrayList<Inscripciones>();
            for (Inscripciones inscripcionesCollectionNewInscripcionesToAttach : inscripcionesCollectionNew) {
                inscripcionesCollectionNewInscripcionesToAttach = em.getReference(inscripcionesCollectionNewInscripcionesToAttach.getClass(), inscripcionesCollectionNewInscripcionesToAttach.getIdInscripcion());
                attachedInscripcionesCollectionNew.add(inscripcionesCollectionNewInscripcionesToAttach);
            }
            inscripcionesCollectionNew = attachedInscripcionesCollectionNew;
            estudiantes.setInscripcionesCollection(inscripcionesCollectionNew);
            estudiantes = em.merge(estudiantes);
            for (Asistencia asistenciaCollectionNewAsistencia : asistenciaCollectionNew) {
                if (!asistenciaCollectionOld.contains(asistenciaCollectionNewAsistencia)) {
                    Estudiantes oldIdEstudianteOfAsistenciaCollectionNewAsistencia = asistenciaCollectionNewAsistencia.getIdEstudiante();
                    asistenciaCollectionNewAsistencia.setIdEstudiante(estudiantes);
                    asistenciaCollectionNewAsistencia = em.merge(asistenciaCollectionNewAsistencia);
                    if (oldIdEstudianteOfAsistenciaCollectionNewAsistencia != null && !oldIdEstudianteOfAsistenciaCollectionNewAsistencia.equals(estudiantes)) {
                        oldIdEstudianteOfAsistenciaCollectionNewAsistencia.getAsistenciaCollection().remove(asistenciaCollectionNewAsistencia);
                        oldIdEstudianteOfAsistenciaCollectionNewAsistencia = em.merge(oldIdEstudianteOfAsistenciaCollectionNewAsistencia);
                    }
                }
            }
            for (Calificaciones calificacionesCollectionNewCalificaciones : calificacionesCollectionNew) {
                if (!calificacionesCollectionOld.contains(calificacionesCollectionNewCalificaciones)) {
                    Estudiantes oldIdEstudianteOfCalificacionesCollectionNewCalificaciones = calificacionesCollectionNewCalificaciones.getIdEstudiante();
                    calificacionesCollectionNewCalificaciones.setIdEstudiante(estudiantes);
                    calificacionesCollectionNewCalificaciones = em.merge(calificacionesCollectionNewCalificaciones);
                    if (oldIdEstudianteOfCalificacionesCollectionNewCalificaciones != null && !oldIdEstudianteOfCalificacionesCollectionNewCalificaciones.equals(estudiantes)) {
                        oldIdEstudianteOfCalificacionesCollectionNewCalificaciones.getCalificacionesCollection().remove(calificacionesCollectionNewCalificaciones);
                        oldIdEstudianteOfCalificacionesCollectionNewCalificaciones = em.merge(oldIdEstudianteOfCalificacionesCollectionNewCalificaciones);
                    }
                }
            }
            for (Inscripciones inscripcionesCollectionNewInscripciones : inscripcionesCollectionNew) {
                if (!inscripcionesCollectionOld.contains(inscripcionesCollectionNewInscripciones)) {
                    Estudiantes oldIdEstudianteOfInscripcionesCollectionNewInscripciones = inscripcionesCollectionNewInscripciones.getIdEstudiante();
                    inscripcionesCollectionNewInscripciones.setIdEstudiante(estudiantes);
                    inscripcionesCollectionNewInscripciones = em.merge(inscripcionesCollectionNewInscripciones);
                    if (oldIdEstudianteOfInscripcionesCollectionNewInscripciones != null && !oldIdEstudianteOfInscripcionesCollectionNewInscripciones.equals(estudiantes)) {
                        oldIdEstudianteOfInscripcionesCollectionNewInscripciones.getInscripcionesCollection().remove(inscripcionesCollectionNewInscripciones);
                        oldIdEstudianteOfInscripcionesCollectionNewInscripciones = em.merge(oldIdEstudianteOfInscripcionesCollectionNewInscripciones);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = estudiantes.getIdEstudiante();
                if (findEstudiantes(id) == null) {
                    throw new NonexistentEntityException("The estudiantes with id " + id + " no longer exists.");
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
            Estudiantes estudiantes;
            try {
                estudiantes = em.getReference(Estudiantes.class, id);
                estudiantes.getIdEstudiante();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The estudiantes with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Asistencia> asistenciaCollectionOrphanCheck = estudiantes.getAsistenciaCollection();
            for (Asistencia asistenciaCollectionOrphanCheckAsistencia : asistenciaCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Estudiantes (" + estudiantes + ") cannot be destroyed since the Asistencia " + asistenciaCollectionOrphanCheckAsistencia + " in its asistenciaCollection field has a non-nullable idEstudiante field.");
            }
            Collection<Calificaciones> calificacionesCollectionOrphanCheck = estudiantes.getCalificacionesCollection();
            for (Calificaciones calificacionesCollectionOrphanCheckCalificaciones : calificacionesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Estudiantes (" + estudiantes + ") cannot be destroyed since the Calificaciones " + calificacionesCollectionOrphanCheckCalificaciones + " in its calificacionesCollection field has a non-nullable idEstudiante field.");
            }
            Collection<Inscripciones> inscripcionesCollectionOrphanCheck = estudiantes.getInscripcionesCollection();
            for (Inscripciones inscripcionesCollectionOrphanCheckInscripciones : inscripcionesCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Estudiantes (" + estudiantes + ") cannot be destroyed since the Inscripciones " + inscripcionesCollectionOrphanCheckInscripciones + " in its inscripcionesCollection field has a non-nullable idEstudiante field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(estudiantes);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Estudiantes> findEstudiantesEntities() {
        return findEstudiantesEntities(true, -1, -1);
    }

    public List<Estudiantes> findEstudiantesEntities(int maxResults, int firstResult) {
        return findEstudiantesEntities(false, maxResults, firstResult);
    }

    private List<Estudiantes> findEstudiantesEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Estudiantes.class));
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

    public Estudiantes findEstudiantes(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Estudiantes.class, id);
        } finally {
            em.close();
        }
    }

    public int getEstudiantesCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Estudiantes> rt = cq.from(Estudiantes.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}

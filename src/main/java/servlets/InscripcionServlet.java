package servlets; // O tu paquete de servlets preferido

import dao.InscripcionesJpaController;
import dao.exceptions.NonexistentEntityException; // No hay IllegalOrphanException para esta entidad
import dto.Cursos;
import dto.Estudiantes;
import dto.Inscripciones;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/InscripcionServlet") // Nombre sugerido
public class InscripcionServlet extends HttpServlet {

    private InscripcionesJpaController inscripcionDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(InscripcionServlet.class.getName());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "TU_PU_AQUI" con el nombre real de tu unidad de persistencia
        try {
            DATE_FORMATTER.setLenient(false);
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            inscripcionDAO = new InscripcionesJpaController(emf);
            LOGGER.info("InscripcionServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar InscripcionServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Inscripciones", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy() de InscripcionServlet.");
        }
        super.destroy();
    }

    // Helper para convertir Inscripcion a JsonObject
    private JsonObject inscripcionToJson(Inscripciones inscripcion) {
        if (inscripcion == null) {
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("idInscripcion", inscripcion.getIdInscripcion());

        if (inscripcion.getFechaInscripcion() != null) {
            builder.add("fechaInscripcion", DATE_FORMATTER.format(inscripcion.getFechaInscripcion()));
        } else {
            builder.addNull("fechaInscripcion");
        }

        if (inscripcion.getIdEstudiante() != null) {
            builder.add("idEstudiante", inscripcion.getIdEstudiante().getIdEstudiante());
            // Opcionalmente, podrías incluir más detalles del estudiante si es necesario
            // builder.add("nombreEstudiante", inscripcion.getIdEstudiante().getNombre() + " " + inscripcion.getIdEstudiante().getApellido());
        } else {
            builder.addNull("idEstudiante"); // Aunque optional=false, defensivo
        }

        if (inscripcion.getIdCurso() != null) {
            builder.add("idCurso", inscripcion.getIdCurso().getIdCurso());
            // Opcionalmente, más detalles del curso
            // builder.add("nombreCurso", inscripcion.getIdCurso().getNombreCurso());
        } else {
            builder.addNull("idCurso"); // Aunque optional=false, defensivo
        }
        
        return builder.build();
    }

    private void sendJsonResponse(HttpServletResponse response, int statusCode, JsonStructure jsonStructure) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(config);
            try (JsonWriter jsonWriter = writerFactory.createWriter(out)) {
                if (jsonStructure instanceof JsonObject) {
                    jsonWriter.writeObject((JsonObject) jsonStructure);
                } else if (jsonStructure instanceof JsonArray) {
                    jsonWriter.writeArray((JsonArray) jsonStructure);
                }
            }
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        JsonObject errorJson = Json.createObjectBuilder().add("error", message).build();
        sendJsonResponse(response, statusCode, errorJson);
        LOGGER.log(Level.WARNING, "Enviando error al cliente (InscripcionServlet) - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "InscripcionServlet: doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Inscripciones inscripcion = inscripcionDAO.findInscripciones(id);
                if (inscripcion != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, inscripcionToJson(inscripcion));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Inscripción no encontrada con ID: " + id);
                }
            } else {
                List<Inscripciones> listaInscripciones = inscripcionDAO.findInscripcionesEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Inscripciones i : listaInscripciones) {
                    arrayBuilder.add(inscripcionToJson(i));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de inscripción inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error crítico en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "InscripcionServlet: doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            // idEstudiante e idCurso son obligatorios
            if (!jsonObject.containsKey("idEstudiante") || jsonObject.isNull("idEstudiante") ||
                !jsonObject.containsKey("idCurso") || jsonObject.isNull("idCurso")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Los campos 'idEstudiante' e 'idCurso' son obligatorios.");
                return;
            }

            int idEstudianteVal;
            int idCursoVal;
            try {
                idEstudianteVal = jsonObject.getInt("idEstudiante");
                idCursoVal = jsonObject.getInt("idCurso");
            } catch (JsonException | ClassCastException e) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idEstudiante' e 'idCurso' deben ser números enteros válidos.");
                return;
            }


            Inscripciones nuevaInscripcion = new Inscripciones();
            
            if (jsonObject.containsKey("fechaInscripcion") && !jsonObject.isNull("fechaInscripcion")) {
                String fechaStr = jsonObject.getString("fechaInscripcion");
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        nuevaInscripcion.setFechaInscripcion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaInscripcion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                } else {
                     nuevaInscripcion.setFechaInscripcion(null); // Si se envía string vacío, tratar como null
                }
            } else {
                // Fecha puede ser null o puedes asignar la fecha actual si lo prefieres
                 nuevaInscripcion.setFechaInscripcion(new Date()); // Opcional: fecha actual por defecto
            }
            
            // Crear instancias proxy para las relaciones
            Estudiantes estudianteRef = new Estudiantes(idEstudianteVal);
            Cursos cursoRef = new Cursos(idCursoVal);
            
            nuevaInscripcion.setIdEstudiante(estudianteRef);
            nuevaInscripcion.setIdCurso(cursoRef);
            
            // Validar que el Estudiante y Curso existan (opcional pero recomendado)
            // EntityManager tempEm = emf.createEntityManager();
            // try {
            //     if (tempEm.find(Estudiantes.class, idEstudianteVal) == null) {
            //         sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El estudiante con ID " + idEstudianteVal + " no existe.");
            //         return;
            //     }
            //     if (tempEm.find(Cursos.class, idCursoVal) == null) {
            //         sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El curso con ID " + idCursoVal + " no existe.");
            //         return;
            //     }
            // } finally {
            //     if (tempEm != null) tempEm.close();
            // }


            inscripcionDAO.create(nuevaInscripcion);
            LOGGER.log(Level.INFO, "InscripcionServlet: Inscripción creada con ID: {0}", nuevaInscripcion.getIdInscripcion());
            // Devolver la inscripción completa (con objetos anidados si es necesario, o al menos los IDs)
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, inscripcionToJson(inscripcionDAO.findInscripciones(nuevaInscripcion.getIdInscripcion())));


        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) { // Capturar otras excepciones como las de constraint violation si los IDs no existen
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error crítico en doPost: " + e.getMessage(), e);
             // Podrías necesitar un manejo más específico si el DAO o la BD lanzan errores por FK no encontradas
            if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: El estudiante o curso especificado no existe.");
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear inscripción: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "InscripcionServlet: doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idInscripcion")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idInscripcion' para la actualización.");
                return;
            }
            int idInscripcion = jsonObject.getInt("idInscripcion");

            Inscripciones inscripcionExistente = inscripcionDAO.findInscripciones(idInscripcion);
            if (inscripcionExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Inscripción no encontrada con ID: " + idInscripcion + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("fechaInscripcion")) {
                if (jsonObject.isNull("fechaInscripcion") || jsonObject.getString("fechaInscripcion").trim().isEmpty()) {
                    inscripcionExistente.setFechaInscripcion(null);
                } else {
                    String fechaStr = jsonObject.getString("fechaInscripcion");
                    try {
                        inscripcionExistente.setFechaInscripcion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaInscripcion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            if (jsonObject.containsKey("idEstudiante")) {
                 if (jsonObject.isNull("idEstudiante")) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'idEstudiante' no puede ser nulo en la actualización.");
                    return;
                 }
                 try {
                    int idEstudianteVal = jsonObject.getInt("idEstudiante");
                    Estudiantes estudianteRef = new Estudiantes(idEstudianteVal);
                    inscripcionExistente.setIdEstudiante(estudianteRef);
                 } catch (JsonException | ClassCastException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idEstudiante' debe ser un número entero válido para la actualización.");
                    return;
                 }
            }
            
            if (jsonObject.containsKey("idCurso")) {
                 if (jsonObject.isNull("idCurso")) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'idCurso' no puede ser nulo en la actualización.");
                    return;
                 }
                 try {
                    int idCursoVal = jsonObject.getInt("idCurso");
                    Cursos cursoRef = new Cursos(idCursoVal);
                    inscripcionExistente.setIdCurso(cursoRef);
                 } catch (JsonException | ClassCastException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idCurso' debe ser un número entero válido para la actualización.");
                    return;
                 }
            }
            
            inscripcionDAO.edit(inscripcionExistente);
            LOGGER.log(Level.INFO, "InscripcionServlet: Inscripción actualizada con ID: {0}", inscripcionExistente.getIdInscripcion());
            sendJsonResponse(response, HttpServletResponse.SC_OK, inscripcionToJson(inscripcionDAO.findInscripciones(inscripcionExistente.getIdInscripcion())));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Intento de editar inscripción inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error crítico en doPut: " + e.getMessage(), e);
            if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: El estudiante o curso especificado no existe.");
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar inscripción: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "InscripcionServlet: doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            inscripcionDAO.destroy(id); 
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Inscripción con ID " + id + " eliminada correctamente.")
                .build();
            LOGGER.log(Level.INFO, "InscripcionServlet: Inscripción eliminada con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de inscripción inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "InscripcionServlet: Intento de eliminar inscripción inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Inscripción no encontrada con ID: " + idParam);
        } catch (Exception e) { // No hay IllegalOrphanException directa en este DAO
            LOGGER.log(Level.SEVERE, "InscripcionServlet: Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar inscripción: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Inscripciones";
    }
}
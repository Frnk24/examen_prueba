package servlet; // O tu paquete de servlets preferido

import dao.ClasesJpaController;
import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dto.Clases;
import dto.Cursos;
import dto.Materias;
import dto.Profesores;

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

@WebServlet("/ClaseServlet") // Nombre sugerido
public class ClaseServlet extends HttpServlet {

    private ClasesJpaController claseDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(ClaseServlet.class.getName());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "TU_PU_AQUI" con el nombre real de tu unidad de persistencia
        try {
            DATE_FORMATTER.setLenient(false);
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            claseDAO = new ClasesJpaController(emf);
            LOGGER.info("ClaseServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar ClaseServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Clases", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy() de ClaseServlet.");
        }
        super.destroy();
    }

    private JsonObject claseToJson(Clases clase) {
        if (clase == null) {
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("idClase", clase.getIdClase());

        if (clase.getFechaClase() != null) {
            builder.add("fechaClase", DATE_FORMATTER.format(clase.getFechaClase()));
        } else {
            builder.addNull("fechaClase");
        }

        if (clase.getIdMateria() != null) {
            builder.add("idMateria", clase.getIdMateria().getIdMateria());
            // Opcional: builder.add("nombreMateria", clase.getIdMateria().getNombreMateria());
        } else {
            builder.addNull("idMateria");
        }

        if (clase.getIdProfesor() != null) {
            builder.add("idProfesor", clase.getIdProfesor().getIdProfesor());
            // Opcional: builder.add("nombreProfesor", clase.getIdProfesor().getNombre() + " " + clase.getIdProfesor().getApellido());
        } else {
            builder.addNull("idProfesor");
        }

        if (clase.getIdCurso() != null) {
            builder.add("idCurso", clase.getIdCurso().getIdCurso());
            // Opcional: builder.add("nombreCurso", clase.getIdCurso().getNombreCurso());
        } else {
            builder.addNull("idCurso");
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
        LOGGER.log(Level.WARNING, "Enviando error al cliente (ClaseServlet) - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "ClaseServlet: doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Clases clase = claseDAO.findClases(id);
                if (clase != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, claseToJson(clase));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Clase no encontrada con ID: " + id);
                }
            } else {
                List<Clases> listaClases = claseDAO.findClasesEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Clases c : listaClases) {
                    arrayBuilder.add(claseToJson(c));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de clase inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error crítico en doGet: " + e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "ClaseServlet: doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idMateria") || jsonObject.isNull("idMateria") ||
                !jsonObject.containsKey("idProfesor") || jsonObject.isNull("idProfesor") ||
                !jsonObject.containsKey("idCurso") || jsonObject.isNull("idCurso")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Los campos 'idMateria', 'idProfesor' e 'idCurso' son obligatorios.");
                return;
            }

            int idMateriaVal, idProfesorVal, idCursoVal;
            try {
                idMateriaVal = jsonObject.getInt("idMateria");
                idProfesorVal = jsonObject.getInt("idProfesor");
                idCursoVal = jsonObject.getInt("idCurso");
            } catch (JsonException | ClassCastException e) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idMateria', 'idProfesor' e 'idCurso' deben ser números enteros válidos.");
                return;
            }

            Clases nuevaClase = new Clases();
            
            if (jsonObject.containsKey("fechaClase") && !jsonObject.isNull("fechaClase")) {
                String fechaStr = jsonObject.getString("fechaClase");
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        nuevaClase.setFechaClase(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaClase inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                } else {
                     nuevaClase.setFechaClase(null);
                }
            } else {
                 nuevaClase.setFechaClase(new Date()); // Opcional: fecha actual por defecto
            }
            
            Materias materiaRef = new Materias(idMateriaVal);
            Profesores profesorRef = new Profesores(idProfesorVal);
            Cursos cursoRef = new Cursos(idCursoVal);
            
            nuevaClase.setIdMateria(materiaRef);
            nuevaClase.setIdProfesor(profesorRef);
            nuevaClase.setIdCurso(cursoRef);
            
            // Aquí podrías agregar validaciones para asegurar que Materia, Profesor y Curso existan
            // similar a como se sugirió en InscripcionServlet, usando un EntityManager temporal.

            claseDAO.create(nuevaClase);
            LOGGER.log(Level.INFO, "ClaseServlet: Clase creada con ID: {0}", nuevaClase.getIdClase());
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, claseToJson(claseDAO.findClases(nuevaClase.getIdClase())));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error crítico en doPost: " + e.getMessage(), e);
            if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: La materia, profesor o curso especificado no existe.");
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear clase: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "ClaseServlet: doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idClase")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idClase' para la actualización.");
                return;
            }
            int idClase = jsonObject.getInt("idClase");

            Clases claseExistente = claseDAO.findClases(idClase);
            if (claseExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Clase no encontrada con ID: " + idClase + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("fechaClase")) {
                if (jsonObject.isNull("fechaClase") || jsonObject.getString("fechaClase").trim().isEmpty()) {
                    claseExistente.setFechaClase(null);
                } else {
                    String fechaStr = jsonObject.getString("fechaClase");
                    try {
                        claseExistente.setFechaClase(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaClase inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            if (jsonObject.containsKey("idMateria")) {
                 if (jsonObject.isNull("idMateria")) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idMateria' no puede ser nulo."); return; }
                 try { claseExistente.setIdMateria(new Materias(jsonObject.getInt("idMateria"))); }
                 catch (JsonException | ClassCastException e) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idMateria' debe ser un número."); return; }
            }
            if (jsonObject.containsKey("idProfesor")) {
                 if (jsonObject.isNull("idProfesor")) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idProfesor' no puede ser nulo."); return; }
                 try { claseExistente.setIdProfesor(new Profesores(jsonObject.getInt("idProfesor"))); }
                 catch (JsonException | ClassCastException e) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idProfesor' debe ser un número."); return; }
            }
            if (jsonObject.containsKey("idCurso")) {
                 if (jsonObject.isNull("idCurso")) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idCurso' no puede ser nulo."); return; }
                 try { claseExistente.setIdCurso(new Cursos(jsonObject.getInt("idCurso"))); }
                 catch (JsonException | ClassCastException e) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idCurso' debe ser un número."); return; }
            }
            
            claseDAO.edit(claseExistente);
            LOGGER.log(Level.INFO, "ClaseServlet: Clase actualizada con ID: {0}", claseExistente.getIdClase());
            sendJsonResponse(response, HttpServletResponse.SC_OK, claseToJson(claseDAO.findClases(claseExistente.getIdClase())));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Intento de editar clase inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error crítico en doPut: " + e.getMessage(), e);
            if (e instanceof IllegalOrphanException) { // Aunque el DAO de Clases no lo lance directamente para sus FKs
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Error de integridad referencial: " + e.getMessage());
            } else if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: La materia, profesor o curso especificado no existe.");
            }
            else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar clase: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "ClaseServlet: doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            claseDAO.destroy(id); 
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Clase con ID " + id + " eliminada correctamente.")
                .build();
            LOGGER.log(Level.INFO, "ClaseServlet: Clase eliminada con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de clase inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "ClaseServlet: Intento de eliminar clase inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Clase no encontrada con ID: " + idParam);
        } catch (IllegalOrphanException e) { // Esta sí la lanza el DAO de Clases
            LOGGER.log(Level.WARNING, "ClaseServlet: Intento de eliminar clase con asistencias o calificaciones: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                    "No se puede eliminar la clase. Tiene asistencias o calificaciones asociadas. Detalles: " + e.getMessage());
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ClaseServlet: Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar clase: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Clases";
    }
}
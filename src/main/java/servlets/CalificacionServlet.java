package servlets; // O tu paquete de servlets preferido

import dao.CalificacionesJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Calificaciones;
import dto.Clases;
import dto.Estudiantes;

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
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/CalificacionServlet") // Nombre sugerido
public class CalificacionServlet extends HttpServlet {

    private CalificacionesJpaController calificacionDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(CalificacionServlet.class.getName());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "TU_PU_AQUI" con el nombre real de tu unidad de persistencia
        try {
            DATE_FORMATTER.setLenient(false);
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            calificacionDAO = new CalificacionesJpaController(emf);
            LOGGER.info("CalificacionServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar CalificacionServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Calificaciones", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy() de CalificacionServlet.");
        }
        super.destroy();
    }

    private JsonObject calificacionToJson(Calificaciones calificacion) {
        if (calificacion == null) {
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("idCalificacion", calificacion.getIdCalificacion());

        if (calificacion.getNota() != null) {
            builder.add("nota", calificacion.getNota()); // BigDecimal se serializa como número
        } else {
            builder.addNull("nota");
        }

        if (calificacion.getFechaCalificacion() != null) {
            builder.add("fechaCalificacion", DATE_FORMATTER.format(calificacion.getFechaCalificacion()));
        } else {
            builder.addNull("fechaCalificacion");
        }

        if (calificacion.getIdEstudiante() != null) {
            builder.add("idEstudiante", calificacion.getIdEstudiante().getIdEstudiante());
        } else {
            builder.addNull("idEstudiante");
        }

        if (calificacion.getIdClase() != null) {
            builder.add("idClase", calificacion.getIdClase().getIdClase());
        } else {
            builder.addNull("idClase");
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
        LOGGER.log(Level.WARNING, "Enviando error al cliente (CalificacionServlet) - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "CalificacionServlet: doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Calificaciones calificacion = calificacionDAO.findCalificaciones(id);
                if (calificacion != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, calificacionToJson(calificacion));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Calificación no encontrada con ID: " + id);
                }
            } else {
                List<Calificaciones> listaCalificaciones = calificacionDAO.findCalificacionesEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Calificaciones c : listaCalificaciones) {
                    arrayBuilder.add(calificacionToJson(c));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de calificación inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error crítico en doGet: " + e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "CalificacionServlet: doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idEstudiante") || jsonObject.isNull("idEstudiante") ||
                !jsonObject.containsKey("idClase") || jsonObject.isNull("idClase")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Los campos 'idEstudiante' e 'idClase' son obligatorios.");
                return;
            }

            int idEstudianteVal, idClaseVal;
            try {
                idEstudianteVal = jsonObject.getInt("idEstudiante");
                idClaseVal = jsonObject.getInt("idClase");
            } catch (JsonException | ClassCastException e) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idEstudiante' e 'idClase' deben ser números enteros válidos.");
                return;
            }

            Calificaciones nuevaCalificacion = new Calificaciones();
            
            if (jsonObject.containsKey("nota") && !jsonObject.isNull("nota")) {
                try {
                    // javax.json.JsonNumber puede ser obtenido y luego convertir
                    JsonValue notaValue = jsonObject.get("nota");
                    if (notaValue.getValueType() == JsonValue.ValueType.NUMBER) {
                        nuevaCalificacion.setNota(((JsonNumber) notaValue).bigDecimalValue());
                    } else if (notaValue.getValueType() == JsonValue.ValueType.STRING) {
                         // Intentar parsear si viene como string
                        nuevaCalificacion.setNota(new BigDecimal(jsonObject.getString("nota")));
                    } else {
                         sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nota' debe ser un número válido.");
                         return;
                    }
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de 'nota' inválido. Debe ser un número decimal.");
                    return;
                }
            } else {
                nuevaCalificacion.setNota(null); // O un valor por defecto si se prefiere
            }
            
            if (jsonObject.containsKey("fechaCalificacion") && !jsonObject.isNull("fechaCalificacion")) {
                String fechaStr = jsonObject.getString("fechaCalificacion");
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        nuevaCalificacion.setFechaCalificacion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaCalificacion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                } else {
                     nuevaCalificacion.setFechaCalificacion(null);
                }
            } else {
                 nuevaCalificacion.setFechaCalificacion(new Date()); // Fecha actual por defecto
            }
            
            nuevaCalificacion.setIdEstudiante(new Estudiantes(idEstudianteVal));
            nuevaCalificacion.setIdClase(new Clases(idClaseVal));
            
            calificacionDAO.create(nuevaCalificacion);
            LOGGER.log(Level.INFO, "CalificacionServlet: Calificación creada con ID: {0}", nuevaCalificacion.getIdCalificacion());
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, calificacionToJson(calificacionDAO.findCalificaciones(nuevaCalificacion.getIdCalificacion())));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error crítico en doPost: " + e.getMessage(), e);
            if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: El estudiante o clase especificado no existe.");
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear calificación: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "CalificacionServlet: doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idCalificacion")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idCalificacion' para la actualización.");
                return;
            }
            int idCalificacion = jsonObject.getInt("idCalificacion");

            Calificaciones calificacionExistente = calificacionDAO.findCalificaciones(idCalificacion);
            if (calificacionExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Calificación no encontrada con ID: " + idCalificacion + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("nota")) {
                if (jsonObject.isNull("nota")) {
                    calificacionExistente.setNota(null);
                } else {
                    try {
                        JsonValue notaValue = jsonObject.get("nota");
                        if (notaValue.getValueType() == JsonValue.ValueType.NUMBER) {
                            calificacionExistente.setNota(((JsonNumber) notaValue).bigDecimalValue());
                        } else if (notaValue.getValueType() == JsonValue.ValueType.STRING) {
                            calificacionExistente.setNota(new BigDecimal(jsonObject.getString("nota")));
                        } else {
                            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nota' debe ser un número válido para la actualización.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de 'nota' inválido para la actualización.");
                        return;
                    }
                }
            }

            if (jsonObject.containsKey("fechaCalificacion")) {
                if (jsonObject.isNull("fechaCalificacion") || jsonObject.getString("fechaCalificacion").trim().isEmpty()) {
                    calificacionExistente.setFechaCalificacion(null);
                } else {
                    String fechaStr = jsonObject.getString("fechaCalificacion");
                    try {
                        calificacionExistente.setFechaCalificacion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaCalificacion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            if (jsonObject.containsKey("idEstudiante")) {
                 if (jsonObject.isNull("idEstudiante")) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idEstudiante' no puede ser nulo."); return; }
                 try { calificacionExistente.setIdEstudiante(new Estudiantes(jsonObject.getInt("idEstudiante"))); }
                 catch (JsonException | ClassCastException e) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idEstudiante' debe ser un número."); return; }
            }
            if (jsonObject.containsKey("idClase")) {
                 if (jsonObject.isNull("idClase")) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idClase' no puede ser nulo."); return; }
                 try { calificacionExistente.setIdClase(new Clases(jsonObject.getInt("idClase"))); }
                 catch (JsonException | ClassCastException e) { sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "'idClase' debe ser un número."); return; }
            }
            
            calificacionDAO.edit(calificacionExistente);
            LOGGER.log(Level.INFO, "CalificacionServlet: Calificación actualizada con ID: {0}", calificacionExistente.getIdCalificacion());
            sendJsonResponse(response, HttpServletResponse.SC_OK, calificacionToJson(calificacionDAO.findCalificaciones(calificacionExistente.getIdCalificacion())));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Intento de editar calificación inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error crítico en doPut: " + e.getMessage(), e);
            if (e.getCause() instanceof javax.persistence.PersistenceException && e.getCause().getMessage().contains("ConstraintViolationException")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error de referencia: El estudiante o clase especificado no existe.");
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar calificación: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "CalificacionServlet: doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            calificacionDAO.destroy(id); 
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Calificación con ID " + id + " eliminada correctamente.")
                .build();
            LOGGER.log(Level.INFO, "CalificacionServlet: Calificación eliminada con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de calificación inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "CalificacionServlet: Intento de eliminar calificación inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Calificación no encontrada con ID: " + idParam);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CalificacionServlet: Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar calificación: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Calificaciones";
    }
}
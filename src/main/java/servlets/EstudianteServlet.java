/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlets; // O tu paquete de servlets preferido

import dao.EstudiantesJpaController;
import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dto.Estudiantes;
// No es necesario importar dto.Asistencia, dto.Calificaciones, dto.Inscripciones
// si no se manipulan activamente aquí.

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

@WebServlet("/EstudianteServlet") // Nombre sugerido
public class EstudianteServlet extends HttpServlet {

    private EstudiantesJpaController estudianteDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(EstudianteServlet.class.getName());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            DATE_FORMATTER.setLenient(false); // Para parseo estricto de fechas
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            estudianteDAO = new EstudiantesJpaController(emf);
            LOGGER.info("EstudianteServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar EstudianteServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Estudiantes", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy() de EstudianteServlet.");
        }
        super.destroy();
    }

    // Helper para convertir Estudiante a JsonObject
    private JsonObject estudianteToJson(Estudiantes estudiante) {
        if (estudiante == null) {
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("idEstudiante", estudiante.getIdEstudiante());

        if (estudiante.getNombre() != null) {
            builder.add("nombre", estudiante.getNombre());
        } else {
            builder.addNull("nombre"); 
        }

        if (estudiante.getApellido() != null) {
            builder.add("apellido", estudiante.getApellido());
        } else {
            builder.addNull("apellido");
        }
        
        if (estudiante.getFechaNacimiento() != null) {
            builder.add("fechaNacimiento", DATE_FORMATTER.format(estudiante.getFechaNacimiento()));
        } else {
            builder.addNull("fechaNacimiento");
        }

        if (estudiante.getCorreo() != null) {
            builder.add("correo", estudiante.getCorreo());
        } else {
            builder.addNull("correo");
        }
        
        if (estudiante.getTelefono() != null) {
            builder.add("telefono", estudiante.getTelefono());
        } else {
            builder.addNull("telefono");
        }
        
        // No incluimos las colecciones para simplificar el CRUD
        return builder.build();
    }

    // Helper para enviar respuestas JSON
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
        LOGGER.log(Level.WARNING, "Enviando error al cliente (EstudianteServlet) - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "EstudianteServlet: doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Estudiantes estudiante = estudianteDAO.findEstudiantes(id);
                if (estudiante != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, estudianteToJson(estudiante));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado con ID: " + id);
                }
            } else {
                List<Estudiantes> listaEstudiantes = estudianteDAO.findEstudiantesEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Estudiantes e : listaEstudiantes) {
                    arrayBuilder.add(estudianteToJson(e));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error crítico en doGet: " + e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "EstudianteServlet: doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            String nombre = jsonObject.getString("nombre", null);
            String apellido = jsonObject.getString("apellido", null);
            
            if (nombre == null || nombre.trim().isEmpty() || apellido == null || apellido.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Los campos 'nombre' y 'apellido' son obligatorios.");
                return;
            }

            Estudiantes nuevoEstudiante = new Estudiantes();
            nuevoEstudiante.setNombre(nombre);
            nuevoEstudiante.setApellido(apellido);
            
            if (jsonObject.containsKey("fechaNacimiento") && !jsonObject.isNull("fechaNacimiento")) {
                String fechaStr = jsonObject.getString("fechaNacimiento");
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        nuevoEstudiante.setFechaNacimiento(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaNacimiento inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                } else {
                     nuevoEstudiante.setFechaNacimiento(null); // Si se envía string vacío, tratar como null
                }
            } else {
                 nuevoEstudiante.setFechaNacimiento(null); // Si no viene la key o es JsonNull
            }
            
            nuevoEstudiante.setCorreo(jsonObject.getString("correo", null)); // Permite null
            nuevoEstudiante.setTelefono(jsonObject.getString("telefono", null)); // Permite null
            
            estudianteDAO.create(nuevoEstudiante);
            LOGGER.log(Level.INFO, "EstudianteServlet: Estudiante creado con ID: {0}", nuevoEstudiante.getIdEstudiante());
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, estudianteToJson(nuevoEstudiante));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error crítico en doPost: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear estudiante: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "EstudianteServlet: doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idEstudiante")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idEstudiante' para la actualización.");
                return;
            }
            int idEstudiante = jsonObject.getInt("idEstudiante");

            Estudiantes estudianteExistente = estudianteDAO.findEstudiantes(idEstudiante);
            if (estudianteExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado con ID: " + idEstudiante + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("nombre")) {
                 String nombre = jsonObject.getString("nombre");
                 if(nombre == null || nombre.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombre' no puede estar vacío en la actualización.");
                    return;
                 }
                estudianteExistente.setNombre(nombre);
            }
            if (jsonObject.containsKey("apellido")) {
                 String apellido = jsonObject.getString("apellido");
                 if(apellido == null || apellido.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'apellido' no puede estar vacío en la actualización.");
                    return;
                 }
                estudianteExistente.setApellido(apellido);
            }
            
            if (jsonObject.containsKey("fechaNacimiento")) {
                if (jsonObject.isNull("fechaNacimiento") || jsonObject.getString("fechaNacimiento").trim().isEmpty()) {
                    estudianteExistente.setFechaNacimiento(null);
                } else {
                    String fechaStr = jsonObject.getString("fechaNacimiento");
                    try {
                        estudianteExistente.setFechaNacimiento(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaNacimiento inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            if (jsonObject.containsKey("correo")) {
                estudianteExistente.setCorreo(jsonObject.isNull("correo") ? null : jsonObject.getString("correo"));
            }
            if (jsonObject.containsKey("telefono")) {
                estudianteExistente.setTelefono(jsonObject.isNull("telefono") ? null : jsonObject.getString("telefono"));
            }
            
            estudianteDAO.edit(estudianteExistente);
            LOGGER.log(Level.INFO, "EstudianteServlet: Estudiante actualizado con ID: {0}", estudianteExistente.getIdEstudiante());
            sendJsonResponse(response, HttpServletResponse.SC_OK, estudianteToJson(estudianteExistente));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Intento de editar estudiante inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error crítico en doPut: " + e.getMessage(), e);
             if (e instanceof IllegalOrphanException) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Error de integridad referencial: " + e.getMessage());
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar estudiante: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "EstudianteServlet: doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            estudianteDAO.destroy(id); 
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Estudiante con ID " + id + " eliminado correctamente.")
                .build();
            LOGGER.log(Level.INFO, "EstudianteServlet: Estudiante eliminado con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Intento de eliminar estudiante inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado con ID: " + idParam);
        } catch (IllegalOrphanException e) {
            LOGGER.log(Level.WARNING, "EstudianteServlet: Intento de eliminar estudiante con registros asociados: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                    "No se puede eliminar el estudiante. Tiene registros asociados (asistencias, calificaciones o inscripciones). Detalles: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "EstudianteServlet: Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar estudiante: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Estudiantes";
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlets; // O el paquete que prefieras

import dao.ProfesoresJpaController;
import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dto.Profesores;
// dto.Clases no se necesita importar directamente aquí si no se manipula activamente

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

@WebServlet("/ProfesorServlet")
public class ProfesorServlet extends HttpServlet {

    private ProfesoresJpaController profesorDAO;
    private EntityManagerFactory emf;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "PU" con el nombre de tu unidad de persistencia
        emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); // Reemplazar "PU"
        profesorDAO = new ProfesoresJpaController(emf);
        DATE_FORMATTER.setLenient(false); // Para que el parseo de fechas sea estricto
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        super.destroy();
    }

    // Helper para convertir Profesor a JsonObject
    private JsonObject profesorToJson(Profesores profesor) {
        if (profesor == null) {
            return Json.createObjectBuilder().build();
        }
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("idProfesor", profesor.getIdProfesor());
        if (profesor.getNombre()!=null) {
            builder.add("nombre",profesor.getNombre());
        }else{
            builder.addNull("nombre");
        }
        if (profesor.getApellido()!=null) {
            builder.add("apellido",profesor.getApellido());
        }else{
            builder.addNull("apellido");
        }
        if (profesor.getFechaContratacion() != null) {
            builder.add("fechaContratacion", DATE_FORMATTER.format(profesor.getFechaContratacion()));
        } else {
            builder.add("fechaContratacion", JsonValue.NULL);
        }
        if (profesor.getCorreo()!=null) {
            builder.add("correo",profesor.getCorreo());
        }else{
            builder.addNull("correo");
        }
        if (profesor.getTelefono()!=null) {
            builder.add("telefono",profesor.getTelefono());
        }else{
            builder.addNull("telefono");
        }
        
        
        // No incluimos clasesCollection para simplificar el CRUD,
        // a menos que sea un requisito explícito gestionarlo.
        return builder.build();
    }

    // Helper para enviar respuestas JSON
    private void sendJsonResponse(HttpServletResponse response, int statusCode, JsonStructure jsonStructure) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true); // Para depuración
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Profesores profesor = profesorDAO.findProfesores(id);
                if (profesor != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, profesorToJson(profesor));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Profesor no encontrado con ID: " + id);
                }
            } else {
                List<Profesores> listaProfesores = profesorDAO.findProfesoresEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Profesores p : listaProfesores) {
                    arrayBuilder.add(profesorToJson(p));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de profesor inválido.");
        } catch (Exception e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error en doGet", e);
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
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error leyendo cuerpo de POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            String nombre = jsonObject.getString("nombre", null);
            String apellido = jsonObject.getString("apellido", null);
            
            if (nombre == null || nombre.trim().isEmpty() || apellido == null || apellido.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Nombre y apellido son obligatorios.");
                return;
            }

            Profesores nuevoProfesor = new Profesores();
            nuevoProfesor.setNombre(nombre);
            nuevoProfesor.setApellido(apellido);
            
            if (jsonObject.containsKey("fechaContratacion") && !jsonObject.isNull("fechaContratacion")) {
                String fechaStr = jsonObject.getString("fechaContratacion");
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        nuevoProfesor.setFechaContratacion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaContratacion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            nuevoProfesor.setCorreo(jsonObject.getString("correo", null));
            nuevoProfesor.setTelefono(jsonObject.getString("telefono", null));
            // clasesCollection se inicializará a una lista vacía por el DAO si es null

            profesorDAO.create(nuevoProfesor);
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, profesorToJson(nuevoProfesor));

        } catch (JsonException e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error parseando JSON en POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error en doPost", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear profesor: " + e.getMessage());
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
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error leyendo cuerpo de PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idProfesor")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta 'idProfesor' para actualizar.");
                return;
            }
            int idProfesor = jsonObject.getInt("idProfesor");

            Profesores profesorExistente = profesorDAO.findProfesores(idProfesor);
            if (profesorExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Profesor no encontrado con ID: " + idProfesor);
                return;
            }

            // Actualizar campos si están presentes
            if (jsonObject.containsKey("nombre")) {
                 String nombre = jsonObject.getString("nombre");
                 if(nombre == null || nombre.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El nombre no puede estar vacío.");
                    return;
                 }
                profesorExistente.setNombre(nombre);
            }
            if (jsonObject.containsKey("apellido")) {
                String apellido = jsonObject.getString("apellido");
                 if(apellido == null || apellido.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El apellido no puede estar vacío.");
                    return;
                 }
                profesorExistente.setApellido(apellido);
            }

            if (jsonObject.containsKey("fechaContratacion")) {
                if (jsonObject.isNull("fechaContratacion") || jsonObject.getString("fechaContratacion").trim().isEmpty()) {
                    profesorExistente.setFechaContratacion(null);
                } else {
                    String fechaStr = jsonObject.getString("fechaContratacion");
                    try {
                        profesorExistente.setFechaContratacion(DATE_FORMATTER.parse(fechaStr));
                    } catch (ParseException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fechaContratacion inválido. Usar YYYY-MM-DD.");
                        return;
                    }
                }
            }
            
            if (jsonObject.containsKey("correo")) {
                profesorExistente.setCorreo(jsonObject.isNull("correo") ? null : jsonObject.getString("correo"));
            }
            if (jsonObject.containsKey("telefono")) {
                profesorExistente.setTelefono(jsonObject.isNull("telefono") ? null : jsonObject.getString("telefono"));
            }
            
            // IMPORTANTE: No modificamos profesorExistente.setClasesCollection() aquí.
            // El método edit del DAO espera que la colección esté, y si no la enviamos,
            // se mantendrá la que ya tiene el profesorExistente.
            // Si se enviara una colección vacía, podría intentar borrar las asociaciones.

            profesorDAO.edit(profesorExistente);
            sendJsonResponse(response, HttpServletResponse.SC_OK, profesorToJson(profesorExistente));

        } catch (JsonException e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error parseando JSON en PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.WARNING, "Intento de editar profesor inexistente", e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { // IllegalOrphanException es una subclase de Exception
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error en doPut", e);
             if (e instanceof IllegalOrphanException) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Error de integridad referencial: " + e.getMessage());
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar profesor: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            profesorDAO.destroy(id);
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Profesor con ID " + id + " eliminado correctamente.")
                .build();
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de profesor inválido.");
        } catch (NonexistentEntityException e) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Profesor no encontrado con ID: " + idParam);
        } catch (IllegalOrphanException e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.WARNING, "Intento de eliminar profesor con clases asociadas", e);
            sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, // 409 Conflict es más apropiado aquí
                    "No se puede eliminar el profesor. Tiene clases asociadas: " + e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(ProfesorServlet.class.getName()).log(Level.SEVERE, "Error en doDelete", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar profesor: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar Profesores";
    }
}
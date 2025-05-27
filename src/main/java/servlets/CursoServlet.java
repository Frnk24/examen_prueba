/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlets; // O tu paquete de servlets preferido

import dao.CursosJpaController;
import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dto.Cursos;
// No es necesario importar dto.Clases, dto.Inscripciones
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/CursoServlet") // Nombre sugerido
public class CursoServlet extends HttpServlet {

    private CursosJpaController cursoDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(CursoServlet.class.getName());

    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            cursoDAO = new CursosJpaController(emf);
            LOGGER.info("CursoServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar CursoServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Cursos", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy() de CursoServlet.");
        }
        super.destroy();
    }

    // Helper para convertir Curso a JsonObject
    private JsonObject cursoToJson(Cursos curso) {
        if (curso == null) {
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("idCurso", curso.getIdCurso());

        if (curso.getNombreCurso() != null) {
            builder.add("nombreCurso", curso.getNombreCurso());
        } else {
            builder.addNull("nombreCurso"); 
        }
        
        // anioAcademico es int, no puede ser null en Java (valor por defecto 0 si no se asigna)
        // Pero el JSON podría no incluirlo si no es relevante, o enviarlo siempre.
        // Aquí lo enviamos siempre.
        builder.add("anioAcademico", curso.getAnioAcademico());
        
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
        LOGGER.log(Level.WARNING, "Enviando error al cliente (CursoServlet) - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "CursoServlet: doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Cursos curso = cursoDAO.findCursos(id);
                if (curso != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, cursoToJson(curso));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Curso no encontrado con ID: " + id);
                }
            } else {
                List<Cursos> listaCursos = cursoDAO.findCursosEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Cursos c : listaCursos) {
                    arrayBuilder.add(cursoToJson(c));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de curso inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CursoServlet: Error crítico en doGet: " + e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "CursoServlet: Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "CursoServlet: doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            String nombreCurso = jsonObject.getString("nombreCurso", null);
            
            if (nombreCurso == null || nombreCurso.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombreCurso' es obligatorio.");
                return;
            }
            if (!jsonObject.containsKey("anioAcademico") || jsonObject.isNull("anioAcademico")) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'anioAcademico' es obligatorio y debe ser un número.");
                return;
            }
            int anioAcademico;
            try {
                anioAcademico = jsonObject.getInt("anioAcademico");
            } catch (JsonException | ClassCastException e) { // javax.json puede lanzar ClassCastException si el tipo no es int
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'anioAcademico' debe ser un número entero válido.");
                return;
            }


            Cursos nuevoCurso = new Cursos();
            nuevoCurso.setNombreCurso(nombreCurso);
            nuevoCurso.setAnioAcademico(anioAcademico);
            
            cursoDAO.create(nuevoCurso);
            LOGGER.log(Level.INFO, "CursoServlet: Curso creado con ID: {0}", nuevoCurso.getIdCurso());
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, cursoToJson(nuevoCurso));

        } catch (JsonException e) { // Captura otras JsonException no manejadas arriba (ej. formato JSON general inválido)
            LOGGER.log(Level.WARNING, "CursoServlet: Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CursoServlet: Error crítico en doPost: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear curso: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "CursoServlet: Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "CursoServlet: doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idCurso")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idCurso' para la actualización.");
                return;
            }
            int idCurso = jsonObject.getInt("idCurso");

            Cursos cursoExistente = cursoDAO.findCursos(idCurso);
            if (cursoExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Curso no encontrado con ID: " + idCurso + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("nombreCurso")) {
                 String nombreCurso = jsonObject.getString("nombreCurso");
                 if(nombreCurso == null || nombreCurso.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombreCurso' no puede estar vacío en la actualización.");
                    return;
                 }
                cursoExistente.setNombreCurso(nombreCurso);
            }
            
            if (jsonObject.containsKey("anioAcademico")) {
                if (jsonObject.isNull("anioAcademico")) {
                     sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'anioAcademico' no puede ser nulo en la actualización y debe ser un número.");
                     return;
                }
                try {
                    cursoExistente.setAnioAcademico(jsonObject.getInt("anioAcademico"));
                } catch (JsonException | ClassCastException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'anioAcademico' debe ser un número entero válido para la actualización.");
                    return;
                }
            }
            
            cursoDAO.edit(cursoExistente);
            LOGGER.log(Level.INFO, "CursoServlet: Curso actualizado con ID: {0}", cursoExistente.getIdCurso());
            sendJsonResponse(response, HttpServletResponse.SC_OK, cursoToJson(cursoExistente));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Intento de editar curso inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "CursoServlet: Error crítico en doPut: " + e.getMessage(), e);
             if (e instanceof IllegalOrphanException) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Error de integridad referencial: " + e.getMessage());
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar curso: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "CursoServlet: doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            cursoDAO.destroy(id); 
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Curso con ID " + id + " eliminado correctamente.")
                .build();
            LOGGER.log(Level.INFO, "CursoServlet: Curso eliminado con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de curso inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Intento de eliminar curso inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Curso no encontrado con ID: " + idParam);
        } catch (IllegalOrphanException e) {
            LOGGER.log(Level.WARNING, "CursoServlet: Intento de eliminar curso con registros asociados: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                    "No se puede eliminar el curso. Tiene clases o inscripciones asociadas. Detalles: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CursoServlet: Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar curso: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Cursos";
    }
}
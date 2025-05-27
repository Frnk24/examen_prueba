/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlets; 

import dao.MateriasJpaController;
import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dto.Materias;
// No es necesario dto.Clases si no se manipula activamente aquí

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

@WebServlet("/MateriaServlet") // Nombre sugerido
public class MateriaServlet extends HttpServlet {

    private MateriasJpaController materiaDAO;
    private EntityManagerFactory emf;
    private static final Logger LOGGER = Logger.getLogger(MateriaServlet.class.getName());

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "TU_PU_AQUI" con el nombre real de tu unidad de persistencia
        // definido en persistence.xml
        try {
            emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"); 
            materiaDAO = new MateriasJpaController(emf);
            LOGGER.info("MateriaServlet inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar MateriaServlet: " + e.getMessage(), e);
            throw new ServletException("Error al inicializar el servlet de Materias", e);
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            LOGGER.info("EntityManagerFactory cerrado en destroy().");
        }
        super.destroy();
    }

    // Helper para convertir Materia a JsonObject
    private JsonObject materiaToJson(Materias materia) {
        if (materia == null) {
            // Podría ser JsonValue.NULL o un objeto vacío si se prefiere
            return Json.createObjectBuilder().build(); 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("idMateria", materia.getIdMateria());

        if (materia.getNombreMateria() != null) {
            builder.add("nombreMateria", materia.getNombreMateria());
        } else {
            builder.addNull("nombreMateria"); // Aunque es @NotNull, buena práctica defensiva
        }

        if (materia.getDescripcion() != null) {
            builder.add("descripcion", materia.getDescripcion());
        } else {
            builder.addNull("descripcion");
        }
        
        // No incluimos clasesCollection para simplificar el CRUD
        return builder.build();
    }

    // Helper para enviar respuestas JSON
    private void sendJsonResponse(HttpServletResponse response, int statusCode, JsonStructure jsonStructure) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8"); // Asegura UTF-8 para caracteres especiales
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
        LOGGER.log(Level.WARNING, "Enviando error al cliente - Status: {0}, Mensaje: {1}", new Object[]{statusCode, message});
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); // Asegura UTF-8 para parámetros
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "doGet solicitado. ID: {0}", idParam);

        try {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Materias materia = materiaDAO.findMaterias(id);
                if (materia != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, materiaToJson(materia));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Materia no encontrada con ID: " + id);
                }
            } else {
                List<Materias> listaMaterias = materiaDAO.findMateriasEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Materias m : listaMaterias) {
                    arrayBuilder.add(materiaToJson(m));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error de formato de número en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de materia inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico en doGet: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); // Para el cuerpo de la solicitud
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error leyendo cuerpo de POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "doPost solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            String nombreMateria = jsonObject.getString("nombreMateria", null);
            
            if (nombreMateria == null || nombreMateria.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombreMateria' es obligatorio.");
                return;
            }

            Materias nuevaMateria = new Materias();
            nuevaMateria.setNombreMateria(nombreMateria);
            
            // Descripcion es opcional
            if (jsonObject.containsKey("descripcion") && !jsonObject.isNull("descripcion")) {
                nuevaMateria.setDescripcion(jsonObject.getString("descripcion"));
            } else {
                nuevaMateria.setDescripcion(null); // o "" si se prefiere string vacío en DB
            }
            
            materiaDAO.create(nuevaMateria);
            LOGGER.log(Level.INFO, "Materia creada con ID: {0}", nuevaMateria.getIdMateria());
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, materiaToJson(nuevaMateria));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "Error parseando JSON en POST: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico en doPost: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear materia: " + e.getMessage());
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
            LOGGER.log(Level.SEVERE, "Error leyendo cuerpo de PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }
        
        LOGGER.log(Level.INFO, "doPut solicitado con cuerpo: {0}", sb.toString());

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idMateria")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idMateria' para la actualización.");
                return;
            }
            int idMateria = jsonObject.getInt("idMateria");

            Materias materiaExistente = materiaDAO.findMaterias(idMateria);
            if (materiaExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Materia no encontrada con ID: " + idMateria + " para actualizar.");
                return;
            }

            if (jsonObject.containsKey("nombreMateria")) {
                 String nombreMateria = jsonObject.getString("nombreMateria");
                 if(nombreMateria == null || nombreMateria.trim().isEmpty()){
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombreMateria' no puede estar vacío en la actualización.");
                    return;
                 }
                materiaExistente.setNombreMateria(nombreMateria);
            }
            
            // Si 'descripcion' está presente en el JSON, se actualiza.
            // Si se quiere poder borrar la descripción enviando null, se maneja así:
            if (jsonObject.containsKey("descripcion")) {
                if (jsonObject.isNull("descripcion") || jsonObject.getString("descripcion").trim().isEmpty()) {
                    materiaExistente.setDescripcion(null); // O "" si prefieres no nulos en DB para strings
                } else {
                    materiaExistente.setDescripcion(jsonObject.getString("descripcion"));
                }
            }
            // Si 'descripcion' no está en el JSON, no se toca el valor existente.

            materiaDAO.edit(materiaExistente);
            LOGGER.log(Level.INFO, "Materia actualizada con ID: {0}", materiaExistente.getIdMateria());
            sendJsonResponse(response, HttpServletResponse.SC_OK, materiaToJson(materiaExistente));

        } catch (JsonException e) {
            LOGGER.log(Level.WARNING, "Error parseando JSON en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "Intento de editar materia inexistente en PUT: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) { // Captura genérica para otras como IllegalOrphanException
            LOGGER.log(Level.SEVERE, "Error crítico en doPut: " + e.getMessage(), e);
             if (e instanceof IllegalOrphanException) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Error de integridad referencial: " + e.getMessage());
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar materia: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");
        LOGGER.log(Level.INFO, "doDelete solicitado. ID: {0}", idParam);

        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'id' para eliminar.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            materiaDAO.destroy(id); // Lanza excepciones si no existe o hay huérfanos
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Materia con ID " + id + " eliminada correctamente.")
                .build();
            LOGGER.log(Level.INFO, "Materia eliminada con ID: {0}", id);
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error de formato de número en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de materia inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "Intento de eliminar materia inexistente en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Materia no encontrada con ID: " + idParam);
        } catch (IllegalOrphanException e) {
            LOGGER.log(Level.WARNING, "Intento de eliminar materia con clases asociadas: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                    "No se puede eliminar la materia. Tiene clases asociadas. Detalles: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico en doDelete: " + e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar materia: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar la entidad Materias";
    }
}
package servlets; // O el paquete que prefieras para tus servlets

import dao.UsuariosJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Roles;
import dto.Usuarios;

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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/UsuarioServlet")
public class UsuarioServlet extends HttpServlet {

    private UsuariosJpaController usuarioDAO;
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        super.init();
        
        emf = Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU");
        usuarioDAO = new UsuariosJpaController(emf);
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        super.destroy();
    }

    // Helper para convertir Usuarios a JsonObject
    private JsonObject usuarioToJson(Usuarios usuario) {
        if (usuario == null) {
            return JsonValue.NULL.asJsonObject(); // O lanzar excepción/retornar null según prefieras
        }
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("idUsuario", usuario.getIdUsuario())
                .add("nombreUsuario", usuario.getNombreUsuario())
                .add("correo", usuario.getCorreo());
        // No incluimos contraseña en la respuesta por seguridad general,
        // a menos que sea estrictamente necesario para alguna funcionalidad.
        // .add("contrasena", usuario.getContrasena());

        if (usuario.getIdRol() != null) {
            builder.add("idRol", usuario.getIdRol().getIdRol());
        } else {
            builder.add("idRol", JsonValue.NULL);
        }
        return builder.build();
    }

    // Helper para enviar respuestas JSON
    private void sendJsonResponse(HttpServletResponse response, int statusCode, JsonStructure jsonStructure) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            // Configurar para pretty printing si se desea (para depuración)
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
        JsonObject errorJson = Json.createObjectBuilder()
                .add("error", message)
                .build();
        sendJsonResponse(response, statusCode, errorJson);
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String idParam = request.getParameter("id");

        try {
            if (idParam != null && !idParam.isEmpty()) {
                // Obtener un usuario específico
                int id = Integer.parseInt(idParam);
                Usuarios usuario = usuarioDAO.findUsuarios(id);
                if (usuario != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, usuarioToJson(usuario));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado con ID: " + id);
                }
            } else {
                // Obtener todos los usuarios
                List<Usuarios> listaUsuarios = usuarioDAO.findUsuariosEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Usuarios u : listaUsuarios) {
                    arrayBuilder.add(usuarioToJson(u));
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de usuario inválido.");
        } catch (Exception e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error en doGet", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error leyendo cuerpo de POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            String nombreUsuario = jsonObject.getString("nombreUsuario", null);
            String correo = jsonObject.getString("correo", null);
            String contrasena = jsonObject.getString("contrasena", null);
            int idRol = jsonObject.getInt("idRol", -1); // -1 o algún valor por defecto si no se provee

            if (nombreUsuario == null || correo == null || contrasena == null || idRol == -1) {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Faltan campos obligatorios: nombreUsuario, correo, contrasena, idRol.");
                return;
            }

            Usuarios nuevoUsuario = new Usuarios();
            nuevoUsuario.setNombreUsuario(nombreUsuario);
            nuevoUsuario.setCorreo(correo);
            nuevoUsuario.setContrasena(contrasena); // Considerar hashear la contraseña en una aplicación real

            Roles rol = new Roles(); // Asumimos que existe una clase Roles
            rol.setIdRol(idRol);     // y que se puede instanciar con solo el ID para la relación
            nuevoUsuario.setIdRol(rol);

            usuarioDAO.create(nuevoUsuario);
            // Devolvemos el usuario creado (con su ID generado)
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, usuarioToJson(nuevoUsuario));

        } catch (JsonException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error parseando JSON en POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error en doPost", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear usuario: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error leyendo cuerpo de PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Error al leer los datos de la solicitud.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(sb.toString()))) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("idUsuario")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Falta el campo 'idUsuario' para la actualización.");
                return;
            }
            int idUsuario = jsonObject.getInt("idUsuario");

            Usuarios usuarioExistente = usuarioDAO.findUsuarios(idUsuario);
            if (usuarioExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado con ID: " + idUsuario + " para actualizar.");
                return;
            }

            // Actualizar campos si están presentes en el JSON
            if (jsonObject.containsKey("nombreUsuario")) {
                usuarioExistente.setNombreUsuario(jsonObject.getString("nombreUsuario"));
            }
            if (jsonObject.containsKey("correo")) {
                usuarioExistente.setCorreo(jsonObject.getString("correo"));
            }
            if (jsonObject.containsKey("contrasena") && !jsonObject.getString("contrasena").isEmpty()) {
                // Solo actualizar contraseña si se proporciona una nueva.
                // Podrías tener una lógica más compleja aquí (ej. campo "nuevaContrasena")
                usuarioExistente.setContrasena(jsonObject.getString("contrasena"));
            }
            if (jsonObject.containsKey("idRol")) {
                int idRol = jsonObject.getInt("idRol");
                Roles rol = new Roles();
                rol.setIdRol(idRol);
                usuarioExistente.setIdRol(rol);
            }

            usuarioDAO.edit(usuarioExistente);
            sendJsonResponse(response, HttpServletResponse.SC_OK, usuarioToJson(usuarioExistente));

        } catch (JsonException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error parseando JSON en PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido: " + e.getMessage());
        } catch (NonexistentEntityException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.WARNING, "Intento de editar usuario inexistente", e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error en doPut", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al actualizar usuario: " + e.getMessage());
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
            usuarioDAO.destroy(id); // Lanza NonexistentEntityException si no existe
            
            JsonObject successJson = Json.createObjectBuilder()
                .add("message", "Usuario con ID " + id + " eliminado correctamente.")
                .build();
            sendJsonResponse(response, HttpServletResponse.SC_OK, successJson);

        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de usuario inválido.");
        } catch (NonexistentEntityException e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.WARNING, "Intento de eliminar usuario inexistente", e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado con ID: " + idParam + " para eliminar.");
        } catch (Exception e) {
            Logger.getLogger(UsuarioServlet.class.getName()).log(Level.SEVERE, "Error en doDelete", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al eliminar usuario: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para gestionar Usuarios";
    }
}
package servlets;

import dao.RolesJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Roles;

import java.io.IOException;
import java.util.List;

import javax.json.*;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/RolesServlet")
public class RolesServlet extends HttpServlet {

    private RolesJpaController dao = new RolesJpaController(
        Persistence.createEntityManagerFactory("com.mycompany_prueba_examen01_war_1.0-SNAPSHOTPU"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        List<Roles> rolesList = dao.findRolesEntities();
        for (Roles rol : rolesList) {
            arrayBuilder.add(Json.createObjectBuilder()
                    .add("idRol", rol.getIdRol())
                    .add("nombreRol", rol.getNombreRol()));
        }
        JsonArray jsonArray = arrayBuilder.build();
        response.getWriter().print(jsonArray.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        JsonReader reader = Json.createReader(request.getInputStream());
        JsonObject jsonObject = reader.readObject();
        String nombreRol = jsonObject.getString("nombreRol");

        Roles rol = new Roles();
        rol.setNombreRol(nombreRol);
        dao.create(rol);

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(Json.createObjectBuilder()
            .add("message", "Rol creado correctamente")
            .build()
            .toString());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        JsonReader reader = Json.createReader(request.getInputStream());
        JsonObject jsonObject = reader.readObject();
        int idRol = jsonObject.getInt("idRol");
        String nombreRol = jsonObject.getString("nombreRol");

        try {
            Roles rol = dao.findRoles(idRol);
            if (rol != null) {
                rol.setNombreRol(nombreRol);
                dao.edit(rol);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().print(Json.createObjectBuilder()
                    .add("message", "Rol actualizado")
                    .build()
                    .toString());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        JsonReader reader = Json.createReader(request.getInputStream());
        JsonObject jsonObject = reader.readObject();
        int idRol = jsonObject.getInt("idRol");

        try {
            dao.destroy(idRol);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print(Json.createObjectBuilder()
                .add("message", "Rol eliminado")
                .build()
                .toString());
        } catch (NonexistentEntityException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

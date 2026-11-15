import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/RegisterServlet"})
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        response.setContentType("text/html;charset=UTF-8");

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user);
            stmt.setString(2, pass);
            stmt.executeUpdate();

            try (PrintWriter out = response.getWriter()) {
                out.println("<div class='success-banner'>Account created! You can now sign in.</div>");
                request.getRequestDispatcher("index.html").include(request, response);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<div class='error-banner'>Username already taken. Try another.</div>");
                request.getRequestDispatcher("register.html").include(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try (PrintWriter out = response.getWriter()) {
                out.println("<div class='error-banner'>Database error: " + e.getMessage() + "</div>");
                request.getRequestDispatcher("register.html").include(request, response);
            }
        }
    }
}
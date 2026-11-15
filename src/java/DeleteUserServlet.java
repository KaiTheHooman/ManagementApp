import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/DeleteUserServlet"})
public class DeleteUserServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Security check: only "admin" is allowed to revoke access
        if (session == null || !"admin".equals(session.getAttribute("username"))) {
            response.sendRedirect("index.html");
            return;
        }

        String userIdParam = request.getParameter("userId");

        if (userIdParam != null && !userIdParam.isEmpty()) {
            // Extra safeguard: never allow deleting the 'admin' account itself
            String sql = "DELETE FROM users WHERE id = ? AND username <> 'admin'";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, Integer.parseInt(userIdParam));
                stmt.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Reload the dashboard so the updated user list displays immediately
       response.sendRedirect("DashboardServlet?tab=users");
    }
}
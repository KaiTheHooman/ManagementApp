import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        response.setContentType("text/html;charset=UTF-8");

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user);
            stmt.setString(2, pass);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HttpSession session = request.getSession();
                    session.setAttribute("username", user);

                    response.setHeader("Refresh", "2; URL=DashboardServlet");

                    try (PrintWriter out = response.getWriter()) {
                        out.println("<!DOCTYPE html>");
                        out.println("<html><head><meta charset='UTF-8'><title>Welcome</title>");
                        out.println("<style>");
                        out.println("* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; }");
                        out.println("body { background-color: #f0f2f5; min-height: 100vh; display: flex; justify-content: center; align-items: center; }");
                        out.println(".card { background: #fff; padding: 36px 32px; border-radius: 10px; box-shadow: 0 4px 16px rgba(0,0,0,0.08); width: 100%; max-width: 380px; text-align: center; }");
                        out.println("h2 { color: #1a202c; margin-bottom: 12px; }");
                        out.println(".spinner { margin: 20px auto; width: 36px; height: 36px; border: 4px solid #e2e8f0; border-top-color: #3182ce; border-radius: 50%; animation: spin 0.8s linear infinite; }");
                        out.println("@keyframes spin { to { transform: rotate(360deg); } }");
                        out.println(".subtext { color: #718096; font-size: 14px; margin-bottom: 16px; }");
                        out.println("a { color: #3182ce; text-decoration: none; font-size: 14px; font-weight: 600; }");
                        out.println("a:hover { text-decoration: underline; }");
                        out.println("</style></head><body>");
                        out.println("<div class='card'>");
                        out.println("<h2>Welcome, " + user + "!</h2>");
                        out.println("<div class='spinner'></div>");
                        out.println("<p class='subtext'>Redirecting you to your dashboard...</p>");
                        out.println("<a href='DashboardServlet'>Continue now</a>");
                        out.println("</div></body></html>");
                    }
                } else {
                    try (PrintWriter out = response.getWriter()) {
                        out.println("<div class='error-banner'>Invalid username or password.</div>");
                        request.getRequestDispatcher("index.html").include(request, response);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try (PrintWriter out = response.getWriter()) {
                out.println("<div class='error-banner'>Database error: " + e.getMessage() + "</div>");
                request.getRequestDispatcher("index.html").include(request, response);
            }
        }
    }
}
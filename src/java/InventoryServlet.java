import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/InventoryServlet"})
public class InventoryServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");

        try (Connection conn = DBUtil.getConnection()) {

            if ("add".equals(action)) {
                String productName = request.getParameter("productName").trim();
                String category = request.getParameter("category").trim();
                int stock = Integer.parseInt(request.getParameter("stock"));
                int threshold = Integer.parseInt(request.getParameter("threshold"));

                String sql = "INSERT INTO inventory (product_name, category, stock, low_threshold) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, productName);
                    stmt.setString(2, category);
                    stmt.setInt(3, Math.max(0, stock));
                    stmt.setInt(4, Math.max(0, threshold));
                    stmt.executeUpdate();
                }

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                int stock = Integer.parseInt(request.getParameter("stock"));
                int threshold = Integer.parseInt(request.getParameter("threshold"));

                String sql = "UPDATE inventory SET stock = ?, low_threshold = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, Math.max(0, stock));
                    stmt.setInt(2, Math.max(0, threshold));
                    stmt.setInt(3, id);
                    stmt.executeUpdate();
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                String sql = "DELETE FROM inventory WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Preserve any active category filter or sort order after saving
        String filterCategory = request.getParameter("filterCategory");
        String sortStock = request.getParameter("sortStock");
        StringBuilder redirectUrl = new StringBuilder("DashboardServlet?tab=inventory");

        if (filterCategory != null && !filterCategory.isEmpty()) {
            redirectUrl.append("&category=").append(java.net.URLEncoder.encode(filterCategory, "UTF-8"));
        }
        if (sortStock != null && !sortStock.isEmpty()) {
            redirectUrl.append("&sort=").append(sortStock);
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
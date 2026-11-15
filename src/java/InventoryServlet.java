import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
                // Always force category to UPPERCASE
                String category = request.getParameter("category").trim().toUpperCase();
                int addedStock = Math.max(0, Integer.parseInt(request.getParameter("stock")));
                int threshold = Math.max(0, Integer.parseInt(request.getParameter("threshold")));

                // Check if the same product in the same category already exists (case-insensitive)
                String checkSql = "SELECT id, stock FROM inventory WHERE LOWER(product_name) = LOWER(?) AND UPPER(category) = ?";
                int existingId = -1;
                int existingStock = 0;

                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, productName);
                    checkStmt.setString(2, category);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("id");
                            existingStock = rs.getInt("stock");
                        }
                    }
                }

                if (existingId != -1) {
                    // Duplicate found: merge stocks together and update category/threshold
                    String mergeSql = "UPDATE inventory SET stock = ?, low_threshold = ?, category = ? WHERE id = ?";
                    try (PreparedStatement mergeStmt = conn.prepareStatement(mergeSql)) {
                        mergeStmt.setInt(1, existingStock + addedStock);
                        mergeStmt.setInt(2, threshold);
                        mergeStmt.setString(3, category);
                        mergeStmt.setInt(4, existingId);
                        mergeStmt.executeUpdate();
                    }
                } else {
                    // New item: insert normally
                    String insertSql = "INSERT INTO inventory (product_name, category, stock, low_threshold) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, productName);
                        insertStmt.setString(2, category);
                        insertStmt.setInt(3, addedStock);
                        insertStmt.setInt(4, threshold);
                        insertStmt.executeUpdate();
                    }
                }

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                int stock = Math.max(0, Integer.parseInt(request.getParameter("stock")));
                int threshold = Math.max(0, Integer.parseInt(request.getParameter("threshold")));

                String sql = "UPDATE inventory SET stock = ?, low_threshold = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, stock);
                    stmt.setInt(2, threshold);
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

        // Preserve active category filter and sort order
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
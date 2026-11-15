import java.io.IOException;
import java.math.BigDecimal;
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

        String currentUser = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        try (Connection conn = DBUtil.getConnection()) {

            if ("add".equals(action)) {
                String productId = request.getParameter("productId").trim().toUpperCase();
                String productName = request.getParameter("productName").trim();
                String category = request.getParameter("category").trim().toUpperCase();
                int addedStock = Math.max(0, Integer.parseInt(request.getParameter("stock")));
                int threshold = Math.max(0, Integer.parseInt(request.getParameter("threshold")));
                BigDecimal unitPrice = new BigDecimal(request.getParameter("unitPrice")).max(BigDecimal.ZERO);

                String checkSql = "SELECT id, product_id, product_name, stock FROM inventory "
                                + "WHERE UPPER(product_id) = ? OR (LOWER(product_name) = LOWER(?) AND UPPER(category) = ?)";
                int existingId = -1;
                String existingProdId = productId;
                String existingProdName = productName;
                int existingStock = 0;

                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, productId);
                    checkStmt.setString(2, productName);
                    checkStmt.setString(3, category);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt("id");
                            existingProdId = rs.getString("product_id");
                            existingProdName = rs.getString("product_name");
                            existingStock = rs.getInt("stock");
                        }
                    }
                }

                if (existingId != -1) {
                    int newTotal = existingStock + addedStock;
                    String mergeSql = "UPDATE inventory SET stock = ?, low_threshold = ?, category = ?, unit_price = ? WHERE id = ?";
                    try (PreparedStatement mergeStmt = conn.prepareStatement(mergeSql)) {
                        mergeStmt.setInt(1, newTotal);
                        mergeStmt.setInt(2, threshold);
                        mergeStmt.setString(3, category);
                        mergeStmt.setBigDecimal(4, unitPrice);
                        mergeStmt.setInt(5, existingId);
                        mergeStmt.executeUpdate();
                    }
                    logMovement(conn, existingProdId, existingProdName, "STOCK IN (MERGE)", addedStock, unitPrice, newTotal, currentUser);
                } else {
                    String insertSql = "INSERT INTO inventory (product_id, product_name, category, stock, low_threshold, unit_price) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, productId);
                        insertStmt.setString(2, productName);
                        insertStmt.setString(3, category);
                        insertStmt.setInt(4, addedStock);
                        insertStmt.setInt(5, threshold);
                        insertStmt.setBigDecimal(6, unitPrice);
                        insertStmt.executeUpdate();
                    }
                    logMovement(conn, productId, productName, "INITIAL STOCK", addedStock, unitPrice, addedStock, currentUser);
                }

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                int newStock = Math.max(0, Integer.parseInt(request.getParameter("stock")));
                int threshold = Math.max(0, Integer.parseInt(request.getParameter("threshold")));
                BigDecimal newPrice = new BigDecimal(request.getParameter("unitPrice")).max(BigDecimal.ZERO);

                String prodId = "";
                String prodName = "";
                int oldStock = 0;
                try (PreparedStatement sel = conn.prepareStatement("SELECT product_id, product_name, stock FROM inventory WHERE id = ?")) {
                    sel.setInt(1, id);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            prodId = rs.getString("product_id");
                            prodName = rs.getString("product_name");
                            oldStock = rs.getInt("stock");
                        }
                    }
                }

                String sql = "UPDATE inventory SET stock = ?, low_threshold = ?, unit_price = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, newStock);
                    stmt.setInt(2, threshold);
                    stmt.setBigDecimal(3, newPrice);
                    stmt.setInt(4, id);
                    stmt.executeUpdate();
                }

                if (newStock != oldStock && !prodId.isEmpty()) {
                    int diff = newStock - oldStock;
                    String type = diff > 0 ? "MANUAL IN" : "MANUAL OUT";
                    logMovement(conn, prodId, prodName, type, Math.abs(diff), newPrice, newStock, currentUser);
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));

                String prodId = "";
                String prodName = "";
                int oldStock = 0;
                BigDecimal oldPrice = BigDecimal.ZERO;
                try (PreparedStatement sel = conn.prepareStatement("SELECT product_id, product_name, stock, unit_price FROM inventory WHERE id = ?")) {
                    sel.setInt(1, id);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            prodId = rs.getString("product_id");
                            prodName = rs.getString("product_name");
                            oldStock = rs.getInt("stock");
                            oldPrice = rs.getBigDecimal("unit_price");
                        }
                    }
                }

                String sql = "DELETE FROM inventory WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                }

                if (!prodId.isEmpty()) {
                    logMovement(conn, prodId, prodName, "REMOVED PRODUCT", oldStock, oldPrice, 0, currentUser);
                }

            } else if ("clear_all".equals(action)) {
                if ("admin".equals(currentUser)) {
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM inventory")) {
                        stmt.executeUpdate();
                    }
                    session.removeAttribute("adjCart");
                    logMovement(conn, "ALL", "ENTIRE INVENTORY", "CLEARED ALL (ADMIN RESET)", 0, BigDecimal.ZERO, 0, currentUser);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

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

    private void logMovement(Connection conn, String prodId, String prodName, String type, int qty, BigDecimal unitPrice, int resultingStock, String user) {
        BigDecimal totalValue = unitPrice.multiply(BigDecimal.valueOf(qty));
        String sql = "INSERT INTO stock_logs (product_id, product_name, action_type, qty_changed, unit_price, total_value, resulting_stock, performed_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prodId);
            stmt.setString(2, prodName);
            stmt.setString(3, type);
            stmt.setInt(4, qty);
            stmt.setBigDecimal(5, unitPrice);
            stmt.setBigDecimal(6, totalValue);
            stmt.setInt(7, resultingStock);
            stmt.setString(8, user);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
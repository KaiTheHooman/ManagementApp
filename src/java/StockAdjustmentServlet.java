import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/StockAdjustmentServlet"})
public class StockAdjustmentServlet extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String currentUser = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        // Cart item format: String[]{cartEntryId, productId, productName, mode ("IN" or "OUT"), qty, note}
        List<String[]> cart = (List<String[]>) session.getAttribute("adjCart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("adjCart", cart);
        }

        try (Connection conn = DBUtil.getConnection()) {

            if ("scan".equals(action)) {
                String productId = request.getParameter("productId").trim().toUpperCase();
                String mode = request.getParameter("mode"); // "IN" or "OUT"
                int qty = Math.max(1, Integer.parseInt(request.getParameter("qty")));

                String findSql = "SELECT id, product_name, stock FROM inventory WHERE UPPER(product_id) = ?";
                try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
                    stmt.setString(1, productId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            session.setAttribute("adjMessage", "Error: Product ID '" + productId + "' was not found in inventory.");
                            session.setAttribute("adjMsgType", "error");
                            response.sendRedirect("DashboardServlet?tab=adjustment");
                            return;
                        }

                        int dbId = rs.getInt("id");
                        String prodName = rs.getString("product_name");
                        int currentStock = rs.getInt("stock");
                        String note = "Standard";
                        int newStock = currentStock;

                        if ("IN".equals(mode)) {
                            newStock = currentStock + qty;
                            updateInventoryStock(conn, dbId, newStock);
                            logMovement(conn, productId, prodName, "STOCK IN (CART)", qty, newStock, currentUser);
                            note = "Added +" + qty + " to stock";

                        } else if ("OUT".equals(mode)) {
                            if (currentStock < qty) {
                                // Scan error / discrepancy detected: physical items exist even though system showed empty/insufficient
                                int extraScanned = qty - currentStock;
                                int correctedInitial = currentStock + extraScanned; // Raised to match physical count
                                newStock = 0; // After deducting the scanned 'qty' out

                                updateInventoryStock(conn, dbId, newStock);
                                logMovement(conn, productId, prodName, "SCAN DISCREPANCY FIX", extraScanned, correctedInitial, currentUser);
                                logMovement(conn, productId, prodName, "STOCK OUT (CART)", qty, newStock, currentUser);

                                note = "Discrepancy fixed (+" + extraScanned + " extra found), then -" + qty + " out";
                            } else {
                                newStock = currentStock - qty;
                                updateInventoryStock(conn, dbId, newStock);
                                logMovement(conn, productId, prodName, "STOCK OUT (CART)", qty, newStock, currentUser);
                                note = "Deducted -" + qty + " across platform";
                            }
                        }

                        String entryId = String.valueOf(System.nanoTime());
                        cart.add(new String[]{entryId, productId, prodName, mode, String.valueOf(qty), note});
                        session.setAttribute("adjMessage", "Processed " + prodName + " (" + productId + ") — Current platform stock: " + newStock);
                        session.setAttribute("adjMsgType", "success");
                    }
                }

            } else if ("remove_item".equals(action)) {
                String entryId = request.getParameter("entryId");
                Iterator<String[]> it = cart.iterator();
                while (it.hasNext()) {
                    String[] item = it.next();
                    if (item[0].equals(entryId)) {
                        String prodId = item[1];
                        String prodName = item[2];
                        String mode = item[3];
                        int qty = Integer.parseInt(item[4]);

                        // Reverse the stock adjustment on the database
                        try (PreparedStatement sel = conn.prepareStatement("SELECT id, stock FROM inventory WHERE UPPER(product_id) = ?")) {
                            sel.setString(1, prodId);
                            try (ResultSet rs = sel.executeQuery()) {
                                if (rs.next()) {
                                    int dbId = rs.getInt("id");
                                    int curStock = rs.getInt("stock");
                                    int revertedStock = "IN".equals(mode) ? Math.max(0, curStock - qty) : (curStock + qty);
                                    updateInventoryStock(conn, dbId, revertedStock);
                                    logMovement(conn, prodId, prodName, "CART UNDO (" + mode + ")", qty, revertedStock, currentUser);
                                }
                            }
                        }
                        it.remove();
                        session.setAttribute("adjMessage", "Removed " + prodName + " from cart and reverted stock.");
                        session.setAttribute("adjMsgType", "success");
                        break;
                    }
                }

            } else if ("finalize".equals(action)) {
                int count = cart.size();
                cart.clear();
                session.setAttribute("adjMessage", "Batch finalized (" + count + " adjustment(s) confirmed).");
                session.setAttribute("adjMsgType", "success");
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("adjMessage", "Adjustment error: " + e.getMessage());
            session.setAttribute("adjMsgType", "error");
        }

        response.sendRedirect("DashboardServlet?tab=adjustment");
    }

    private void updateInventoryStock(Connection conn, int id, int newStock) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE inventory SET stock = ? WHERE id = ?")) {
            stmt.setInt(1, newStock);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    private void logMovement(Connection conn, String prodId, String prodName, String type, int qty, int resultingStock, String user) {
        String sql = "INSERT INTO stock_logs (product_id, product_name, action_type, qty_changed, resulting_stock, performed_by) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prodId);
            stmt.setString(2, prodName);
            stmt.setString(3, type);
            stmt.setInt(4, qty);
            stmt.setInt(5, resultingStock);
            stmt.setString(6, user);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
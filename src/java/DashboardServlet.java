import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/DashboardServlet"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String currentUser = (String) session.getAttribute("username");
        String tab = request.getParameter("tab");
        if (tab == null || tab.isEmpty()) {
            tab = "home";
        }

        if ("users".equals(tab) && !"admin".equals(currentUser)) {
            response.sendRedirect("DashboardServlet?tab=home");
            return;
        }

        // 1. Admin: Manage Users
        if ("admin".equals(currentUser) && "users".equals(tab)) {
            List<String[]> userList = new ArrayList<>();
            String sql = "SELECT id, username FROM users ORDER BY id ASC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    userList.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("username")
                    });
                }
                request.setAttribute("userList", userList);

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("dbError", "Could not load users: " + e.getMessage());
            }
        }

        // 2. Manage Inventory (with Product ID, Category Filter & Stock Sort)
        if ("inventory".equals(tab) || "adjustment".equals(tab)) {
            String filterCategory = request.getParameter("category");
            String sortStock = request.getParameter("sort");

            List<String> categories = new ArrayList<>();
            List<String[]> inventoryList = new ArrayList<>();

            try (Connection conn = DBUtil.getConnection()) {

                String catSql = "SELECT DISTINCT UPPER(category) AS cat FROM inventory ORDER BY cat ASC";
                try (PreparedStatement catStmt = conn.prepareStatement(catSql);
                     ResultSet catRs = catStmt.executeQuery()) {
                    while (catRs.next()) {
                        categories.add(catRs.getString("cat"));
                    }
                }

                StringBuilder invSql = new StringBuilder(
                    "SELECT id, product_id, product_name, UPPER(category) AS cat, stock, low_threshold FROM inventory"
                );
                boolean hasCategoryFilter = (filterCategory != null && !filterCategory.isEmpty() && !"ALL".equalsIgnoreCase(filterCategory));

                if (hasCategoryFilter) {
                    invSql.append(" WHERE UPPER(category) = ?");
                }

                if ("stock_asc".equals(sortStock)) {
                    invSql.append(" ORDER BY stock ASC, id ASC");
                } else {
                    invSql.append(" ORDER BY id DESC");
                }

                try (PreparedStatement invStmt = conn.prepareStatement(invSql.toString())) {
                    if (hasCategoryFilter) {
                        invStmt.setString(1, filterCategory.toUpperCase());
                    }
                    try (ResultSet invRs = invStmt.executeQuery()) {
                        while (invRs.next()) {
                            inventoryList.add(new String[]{
                                String.valueOf(invRs.getInt("id")),
                                invRs.getString("product_id"),
                                invRs.getString("product_name"),
                                invRs.getString("cat"),
                                String.valueOf(invRs.getInt("stock")),
                                String.valueOf(invRs.getInt("low_threshold"))
                            });
                        }
                    }
                }

                request.setAttribute("categories", categories);
                request.setAttribute("inventoryList", inventoryList);
                request.setAttribute("selectedCategory", hasCategoryFilter ? filterCategory.toUpperCase() : "ALL");
                request.setAttribute("selectedSort", sortStock != null ? sortStock : "default");

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("invError", "Inventory error: " + e.getMessage());
            }
        }

        // 3. Inventory Summary & Tracking Tab
        if ("summary".equals(tab)) {
            int totalProducts = 0;
            int totalUnits = 0;
            int lowStockCount = 0;
            int outOfStockCount = 0;
            List<String[]> categorySummary = new ArrayList<>();
            List<String[]> stockLogs = new ArrayList<>();

            try (Connection conn = DBUtil.getConnection()) {

                // Overall KPI totals
                String kpiSql = "SELECT COUNT(*) AS prod_cnt, COALESCE(SUM(stock), 0) AS unit_sum, "
                              + "SUM(CASE WHEN stock <= low_threshold AND stock > 0 THEN 1 ELSE 0 END) AS low_cnt, "
                              + "SUM(CASE WHEN stock = 0 THEN 1 ELSE 0 END) AS out_cnt FROM inventory";
                try (PreparedStatement kpiStmt = conn.prepareStatement(kpiSql);
                     ResultSet rs = kpiStmt.executeQuery()) {
                    if (rs.next()) {
                        totalProducts = rs.getInt("prod_cnt");
                        totalUnits = rs.getInt("unit_sum");
                        lowStockCount = rs.getInt("low_cnt");
                        outOfStockCount = rs.getInt("out_cnt");
                    }
                }

                // Category-by-category breakdown
                String catSumSql = "SELECT UPPER(category) AS cat, COUNT(*) AS item_cnt, SUM(stock) AS total_qty "
                                 + "FROM inventory GROUP BY UPPER(category) ORDER BY total_qty DESC";
                try (PreparedStatement catStmt = conn.prepareStatement(catSumSql);
                     ResultSet rs = catStmt.executeQuery()) {
                    while (rs.next()) {
                        categorySummary.add(new String[]{
                            rs.getString("cat"),
                            String.valueOf(rs.getInt("item_cnt")),
                            String.valueOf(rs.getInt("total_qty"))
                        });
                    }
                }

                // Recent stock movement logs (latest 30 entries)
                String logSql = "SELECT product_id, product_name, action_type, qty_changed, resulting_stock, performed_by, created_at "
                              + "FROM stock_logs ORDER BY log_id DESC FETCH FIRST 30 ROWS ONLY";
                try (PreparedStatement logStmt = conn.prepareStatement(logSql);
                     ResultSet rs = logStmt.executeQuery()) {
                    while (rs.next()) {
                        stockLogs.add(new String[]{
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("action_type"),
                            String.valueOf(rs.getInt("qty_changed")),
                            String.valueOf(rs.getInt("resulting_stock")),
                            rs.getString("performed_by"),
                            String.valueOf(rs.getTimestamp("created_at"))
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("summaryError", "Summary error: " + e.getMessage());
            }

            request.setAttribute("totalProducts", totalProducts);
            request.setAttribute("totalUnits", totalUnits);
            request.setAttribute("lowStockCount", lowStockCount);
            request.setAttribute("outOfStockCount", outOfStockCount);
            request.setAttribute("categorySummary", categorySummary);
            request.setAttribute("stockLogs", stockLogs);
        }

        request.setAttribute("activeTab", tab);
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }
}
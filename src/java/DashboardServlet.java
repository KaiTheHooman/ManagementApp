import java.io.IOException;
import java.math.BigDecimal;
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

        // 2. Manage Inventory & Adjustment Lookup
        if ("inventory".equals(tab) || "adjustment".equals(tab)) {
            String filterCategory = request.getParameter("category");
            String sortStock = request.getParameter("sort");

            List<String> categories = new ArrayList<>();
            List<String[]> inventoryList = new ArrayList<>();
            BigDecimal filteredWorth = BigDecimal.ZERO;

            try (Connection conn = DBUtil.getConnection()) {

                String catSql = "SELECT DISTINCT UPPER(category) AS cat FROM inventory ORDER BY cat ASC";
                try (PreparedStatement catStmt = conn.prepareStatement(catSql);
                     ResultSet catRs = catStmt.executeQuery()) {
                    while (catRs.next()) {
                        categories.add(catRs.getString("cat"));
                    }
                }

                StringBuilder invSql = new StringBuilder(
                    "SELECT id, product_id, product_name, UPPER(category) AS cat, stock, low_threshold, unit_price FROM inventory"
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
                            int stk = invRs.getInt("stock");
                            BigDecimal price = invRs.getBigDecimal("unit_price");
                            if (price == null) price = BigDecimal.ZERO;
                            BigDecimal lineWorth = price.multiply(BigDecimal.valueOf(stk));
                            filteredWorth = filteredWorth.add(lineWorth);

                            inventoryList.add(new String[]{
                                String.valueOf(invRs.getInt("id")),
                                invRs.getString("product_id"),
                                invRs.getString("product_name"),
                                invRs.getString("cat"),
                                String.valueOf(stk),
                                String.valueOf(invRs.getInt("low_threshold")),
                                String.format("%.2f", price),
                                String.format("%.2f", lineWorth)
                            });
                        }
                    }
                }

                request.setAttribute("categories", categories);
                request.setAttribute("inventoryList", inventoryList);
                request.setAttribute("filteredWorth", String.format("%.2f", filteredWorth));
                request.setAttribute("selectedCategory", hasCategoryFilter ? filterCategory.toUpperCase() : "ALL");
                request.setAttribute("selectedSort", sortStock != null ? sortStock : "default");

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("invError", "Inventory error: " + e.getMessage());
            }
        }

        // 3. Inventory Summary & Financial Tracking Tab
        if ("summary".equals(tab)) {
            int totalProducts = 0;
            int totalUnits = 0;
            int lowStockCount = 0;
            int outOfStockCount = 0;
            BigDecimal totalInventoryWorth = BigDecimal.ZERO;
            BigDecimal totalWorthIn = BigDecimal.ZERO;
            BigDecimal totalWorthOut = BigDecimal.ZERO;
            List<String[]> categorySummary = new ArrayList<>();
            List<String[]> stockLogs = new ArrayList<>();

            try (Connection conn = DBUtil.getConnection()) {

                // Overall Inventory KPI + Total Worth
                String kpiSql = "SELECT COUNT(*) AS prod_cnt, COALESCE(SUM(stock), 0) AS unit_sum, "
                              + "COALESCE(SUM(stock * unit_price), 0) AS total_worth, "
                              + "SUM(CASE WHEN stock <= low_threshold AND stock > 0 THEN 1 ELSE 0 END) AS low_cnt, "
                              + "SUM(CASE WHEN stock = 0 THEN 1 ELSE 0 END) AS out_cnt FROM inventory";
                try (PreparedStatement kpiStmt = conn.prepareStatement(kpiSql);
                     ResultSet rs = kpiStmt.executeQuery()) {
                    if (rs.next()) {
                        totalProducts = rs.getInt("prod_cnt");
                        totalUnits = rs.getInt("unit_sum");
                        totalInventoryWorth = rs.getBigDecimal("total_worth");
                        lowStockCount = rs.getInt("low_cnt");
                        outOfStockCount = rs.getInt("out_cnt");
                    }
                }

                // Total Monetary Worth IN vs OUT from Stock Logs
                String flowSql = "SELECT "
                               + "COALESCE(SUM(CASE WHEN action_type IN ('INITIAL STOCK', 'STOCK IN (MERGE)', 'STOCK IN (CART)', 'MANUAL IN', 'SCAN DISCREPANCY FIX', 'CART UNDO (OUT)') THEN total_value ELSE 0 END), 0) AS worth_in, "
                               + "COALESCE(SUM(CASE WHEN action_type IN ('STOCK OUT (CART)', 'MANUAL OUT', 'REMOVED PRODUCT', 'CART UNDO (IN)') THEN total_value ELSE 0 END), 0) AS worth_out "
                               + "FROM stock_logs";
                try (PreparedStatement flowStmt = conn.prepareStatement(flowSql);
                     ResultSet rs = flowStmt.executeQuery()) {
                    if (rs.next()) {
                        totalWorthIn = rs.getBigDecimal("worth_in");
                        totalWorthOut = rs.getBigDecimal("worth_out");
                    }
                }

                // Category breakdown with Category Worth
                String catSumSql = "SELECT UPPER(category) AS cat, COUNT(*) AS item_cnt, SUM(stock) AS total_qty, "
                                 + "COALESCE(SUM(stock * unit_price), 0) AS cat_worth "
                                 + "FROM inventory GROUP BY UPPER(category) ORDER BY cat_worth DESC";
                try (PreparedStatement catStmt = conn.prepareStatement(catSumSql);
                     ResultSet rs = catStmt.executeQuery()) {
                    while (rs.next()) {
                        categorySummary.add(new String[]{
                            rs.getString("cat"),
                            String.valueOf(rs.getInt("item_cnt")),
                            String.valueOf(rs.getInt("total_qty")),
                            String.format("%.2f", rs.getBigDecimal("cat_worth"))
                        });
                    }
                }

                // Recent stock movement logs with unit price & total value
                String logSql = "SELECT product_id, product_name, action_type, qty_changed, unit_price, total_value, resulting_stock, performed_by, created_at "
                              + "FROM stock_logs ORDER BY log_id DESC FETCH FIRST 30 ROWS ONLY";
                try (PreparedStatement logStmt = conn.prepareStatement(logSql);
                     ResultSet rs = logStmt.executeQuery()) {
                    while (rs.next()) {
                        stockLogs.add(new String[]{
                            rs.getString("product_id"),
                            rs.getString("product_name"),
                            rs.getString("action_type"),
                            String.valueOf(rs.getInt("qty_changed")),
                            String.format("%.2f", rs.getBigDecimal("unit_price")),
                            String.format("%.2f", rs.getBigDecimal("total_value")),
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
            request.setAttribute("totalInventoryWorth", String.format("%.2f", totalInventoryWorth));
            request.setAttribute("totalWorthIn", String.format("%.2f", totalWorthIn));
            request.setAttribute("totalWorthOut", String.format("%.2f", totalWorthOut));
            request.setAttribute("categorySummary", categorySummary);
            request.setAttribute("stockLogs", stockLogs);
        }

        request.setAttribute("activeTab", tab);
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }
}
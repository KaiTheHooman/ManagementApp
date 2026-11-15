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

        // Security check: only admin can open Manage Users
        if ("users".equals(tab) && !"admin".equals(currentUser)) {
            response.sendRedirect("DashboardServlet?tab=home");
            return;
        }

        // 1. Admin: Load Users List
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

        // 2. All Users: Load Inventory (with optional Category Filter & Stock Sort)
        if ("inventory".equals(tab)) {
            String filterCategory = request.getParameter("category");
            String sortStock = request.getParameter("sort");

            List<String> categories = new ArrayList<>();
            List<String[]> inventoryList = new ArrayList<>();

            try (Connection conn = DBUtil.getConnection()) {

                // Fetch distinct categories created by users for the filter dropdown
                String catSql = "SELECT DISTINCT category FROM inventory ORDER BY category ASC";
                try (PreparedStatement catStmt = conn.prepareStatement(catSql);
                     ResultSet catRs = catStmt.executeQuery()) {
                    while (catRs.next()) {
                        categories.add(catRs.getString("category"));
                    }
                }

                // Build dynamic query for filtering & sorting
                StringBuilder invSql = new StringBuilder("SELECT id, product_name, category, stock, low_threshold FROM inventory");
                boolean hasCategoryFilter = (filterCategory != null && !filterCategory.isEmpty() && !"ALL".equals(filterCategory));

                if (hasCategoryFilter) {
                    invSql.append(" WHERE category = ?");
                }

                if ("stock_asc".equals(sortStock)) {
                    invSql.append(" ORDER BY stock ASC, id ASC");
                } else {
                    invSql.append(" ORDER BY id DESC");
                }

                try (PreparedStatement invStmt = conn.prepareStatement(invSql.toString())) {
                    if (hasCategoryFilter) {
                        invStmt.setString(1, filterCategory);
                    }
                    try (ResultSet invRs = invStmt.executeQuery()) {
                        while (invRs.next()) {
                            inventoryList.add(new String[]{
                                String.valueOf(invRs.getInt("id")),
                                invRs.getString("product_name"),
                                invRs.getString("category"),
                                String.valueOf(invRs.getInt("stock")),
                                String.valueOf(invRs.getInt("low_threshold"))
                            });
                        }
                    }
                }

                request.setAttribute("categories", categories);
                request.setAttribute("inventoryList", inventoryList);
                request.setAttribute("selectedCategory", hasCategoryFilter ? filterCategory : "ALL");
                request.setAttribute("selectedSort", sortStock != null ? sortStock : "default");

            } catch (Exception e) {
                e.printStackTrace();
                request.setAttribute("invError", "Inventory error: " + e.getMessage());
            }
        }

        request.setAttribute("activeTab", tab);
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }
}
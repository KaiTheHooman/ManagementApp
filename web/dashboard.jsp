<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String currentUser = (String) session.getAttribute("username");
    if (currentUser == null) {
        response.sendRedirect("index.html");
        return;
    }
    boolean isAdmin = "admin".equals(currentUser);
    String activeTab = (String) request.getAttribute("activeTab");
    if (activeTab == null) {
        activeTab = "home";
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Dashboard - MyApp Portal</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        body {
            background-color: #f0f2f5;
            color: #333;
            min-height: 100vh;
            display: flex;
            align-items: flex-start;
        }
        /* Sticky Left Sidebar */
        .sidebar {
            width: 240px;
            height: 100vh;
            position: sticky;
            top: 0;
            background-color: #1a202c;
            color: #ffffff;
            display: flex;
            flex-direction: column;
            flex-shrink: 0;
            overflow-y: auto;
            z-index: 20;
        }
        .sidebar-brand {
            padding: 22px 24px;
            font-size: 19px;
            font-weight: 700;
            border-bottom: 1px solid #2d3748;
            letter-spacing: 0.3px;
        }
        .sidebar-menu {
            list-style: none;
            padding: 16px 12px;
            flex: 1;
        }
        .menu-label {
            font-size: 11px;
            text-transform: uppercase;
            color: #718096;
            font-weight: 700;
            padding: 12px 12px 6px;
            letter-spacing: 0.8px;
        }
        .sidebar-menu li a {
            display: block;
            padding: 11px 14px;
            color: #cbd5e0;
            text-decoration: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 500;
            margin-bottom: 4px;
            transition: all 0.2s;
        }
        .sidebar-menu li a:hover {
            background-color: #2d3748;
            color: #ffffff;
        }
        .sidebar-menu li a.active {
            background-color: #3182ce;
            color: #ffffff;
            font-weight: 600;
        }
        .sidebar-footer {
            padding: 16px;
            border-top: 1px solid #2d3748;
        }
        .logout-btn {
            display: block;
            width: 100%;
            text-align: center;
            background-color: #e53e3e;
            color: #ffffff;
            text-decoration: none;
            padding: 10px;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            transition: background-color 0.2s;
        }
        .logout-btn:hover {
            background-color: #c53030;
        }

        /* Main Content Area */
        .main-wrapper {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
            min-height: 100vh;
        }
        /* Sticky Top Bar */
        .topbar {
            position: sticky;
            top: 0;
            z-index: 10;
            background-color: #ffffff;
            padding: 16px 32px;
            border-bottom: 1px solid #e2e8f0;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .page-title {
            font-size: 18px;
            font-weight: 600;
            color: #2d3748;
        }
        .user-info {
            font-size: 14px;
            color: #4a5568;
        }
        .admin-badge {
            background-color: #3182ce;
            color: #ffffff;
            font-size: 11px;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 12px;
            text-transform: uppercase;
            margin-left: 6px;
        }
        .content-area {
            padding: 32px;
            max-width: 1120px;
            width: 100%;
        }

        /* Cards & Grids */
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 20px;
        }
        .card {
            background: #ffffff;
            padding: 24px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            border-top: 4px solid #3182ce;
            margin-bottom: 24px;
        }
        .card.admin-card {
            border-top-color: #38a169;
        }
        .card h2 {
            font-size: 19px;
            color: #1a202c;
            margin-bottom: 6px;
        }
        .card h3 {
            margin-bottom: 8px;
            color: #2d3748;
        }
        .subtext {
            color: #718096;
            font-size: 14px;
            line-height: 1.5;
        }

        /* Inventory Forms & Filter Controls */
        .inv-form-grid {
            display: grid;
            grid-template-columns: 2fr 1.5fr 1fr 1fr auto;
            gap: 12px;
            align-items: end;
            margin-top: 16px;
        }
        .field-box label {
            display: block;
            font-size: 12px;
            font-weight: 600;
            color: #4a5568;
            margin-bottom: 5px;
        }
        .field-box input, .filter-bar select {
            width: 100%;
            padding: 9px 11px;
            border: 1px solid #cbd5e0;
            border-radius: 6px;
            font-size: 14px;
        }
        .field-box input:focus, .filter-bar select:focus {
            outline: none;
            border-color: #3182ce;
        }
        .btn-primary {
            background-color: #3182ce;
            color: #ffffff;
            border: none;
            padding: 10px 18px;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            white-space: nowrap;
        }
        .btn-primary:hover {
            background-color: #2b6cb0;
        }
        .filter-bar {
            display: flex;
            flex-wrap: wrap;
            gap: 14px;
            align-items: center;
            justify-content: space-between;
            margin-top: 16px;
            padding: 12px 16px;
            background-color: #f7fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
        }
        .filter-controls {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            align-items: center;
        }
        .filter-controls label {
            font-size: 13px;
            font-weight: 600;
            color: #4a5568;
        }
        .filter-controls select {
            width: auto;
            min-width: 160px;
            background-color: #ffffff;
        }

        /* Tables & Status Badges */
        .data-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 16px;
            font-size: 14px;
        }
        .data-table th, .data-table td {
            text-align: left;
            padding: 12px 12px;
            border-bottom: 1px solid #e2e8f0;
            vertical-align: middle;
        }
        .data-table th {
            background-color: #f7fafc;
            color: #4a5568;
            font-weight: 600;
        }
        .data-table tr.low-stock-row {
            background-color: #fff5f5;
        }
        .data-table tr:hover {
            background-color: #f7fafc;
        }
        .data-table tr.low-stock-row:hover {
            background-color: #fed7d7;
        }
        .cat-badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
            background-color: #edf2f7;
            color: #2d3748;
        }
        .status-badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 700;
        }
        .status-ok {
            background-color: #c6f6d5;
            color: #22543d;
        }
        .status-low {
            background-color: #fed7d7;
            color: #9b2c2c;
        }
        .num-input {
            width: 75px;
            padding: 6px 8px;
            border: 1px solid #cbd5e0;
            border-radius: 5px;
            font-size: 13px;
        }
        .btn-save {
            background-color: #38a169;
            color: #ffffff;
            border: none;
            padding: 6px 12px;
            border-radius: 5px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-save:hover {
            background-color: #2f855a;
        }
        .btn-danger {
            background-color: #fff5f5;
            color: #c53030;
            border: 1px solid #feb2b2;
            padding: 5px 11px;
            border-radius: 5px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-danger:hover {
            background-color: #e53e3e;
            color: #ffffff;
            border-color: #e53e3e;
        }
        .role-tag {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
            background-color: #edf2f7;
            color: #4a5568;
        }
        .role-tag.admin {
            background-color: #ebf8ff;
            color: #2b6cb0;
        }
        .protected-text {
            color: #a0aec0;
            font-size: 12px;
            font-style: italic;
        }
    </style>
</head>
<body>
    <!-- Sticky Left Navigation Sidebar -->
    <aside class="sidebar">
        <div class="sidebar-brand">MyApp Portal</div>
        <ul class="sidebar-menu">
            <li class="menu-label">Main Menu</li>
            <li>
                <a href="DashboardServlet?tab=home" class="<%= "home".equals(activeTab) ? "active" : "" %>">
                    Home Overview
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=inventory" class="<%= "inventory".equals(activeTab) ? "active" : "" %>">
                    Manage Inventory
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=profile" class="<%= "profile".equals(activeTab) ? "active" : "" %>">
                    My Profile
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=reports" class="<%= "reports".equals(activeTab) ? "active" : "" %>">
                    Reports
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=settings" class="<%= "settings".equals(activeTab) ? "active" : "" %>">
                    Settings
                </a>
            </li>

            <% if (isAdmin) { %>
                <li class="menu-label">Administration</li>
                <li>
                    <a href="DashboardServlet?tab=users" class="<%= "users".equals(activeTab) ? "active" : "" %>">
                        Manage Users
                    </a>
                </li>
            <% } %>
        </ul>
        <div class="sidebar-footer">
            <a href="LogoutServlet" class="logout-btn">Logout</a>
        </div>
    </aside>

    <!-- Right Workspace -->
    <div class="main-wrapper">
        <!-- Sticky Top Bar -->
        <header class="topbar">
            <div class="page-title">
                <% if ("inventory".equals(activeTab)) { %>
                    Inventory Management
                <% } else if ("users".equals(activeTab)) { %>
                    User Access Management
                <% } else if ("profile".equals(activeTab)) { %>
                    My Profile
                <% } else if ("reports".equals(activeTab)) { %>
                    Reports
                <% } else if ("settings".equals(activeTab)) { %>
                    Settings
                <% } else { %>
                    Dashboard Overview
                <% } %>
            </div>
            <div class="user-info">
                Signed in as <strong><%= currentUser %></strong>
                <% if (isAdmin) { %><span class="admin-badge">Admin</span><% } %>
            </div>
        </header>

        <main class="content-area">
            <% if ("inventory".equals(activeTab)) {
                List<String> categories = (List<String>) request.getAttribute("categories");
                List<String[]> inventoryList = (List<String[]>) request.getAttribute("inventoryList");
                String selectedCategory = (String) request.getAttribute("selectedCategory");
                String selectedSort = (String) request.getAttribute("selectedSort");
                String invError = (String) request.getAttribute("invError");
            %>
                <!-- 1. Add New Product Card -->
                <div class="card">
                    <h2>Add New Product</h2>
                    <p class="subtext">Enter a product, assign a required category label, and set your custom low-stock alert threshold.</p>

                    <form action="InventoryServlet" method="POST" class="inv-form-grid">
                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="filterCategory" value="<%= selectedCategory %>">
                        <input type="hidden" name="sortStock" value="<%= selectedSort %>">

                        <div class="field-box">
                            <label for="productName">Product Name *</label>
                            <input type="text" id="productName" name="productName" placeholder="e.g. Wireless Mouse" required>
                        </div>

                        <div class="field-box">
                            <label for="category">Category (Required) *</label>
                            <input type="text" id="category" name="category" list="categorySuggestions" placeholder="e.g. Electronics" required>
                            <datalist id="categorySuggestions">
                                <% if (categories != null) {
                                    for (String cat : categories) { %>
                                        <option value="<%= cat %>">
                                <%  }
                                } %>
                            </datalist>
                        </div>

                        <div class="field-box">
                            <label for="stock">Stock Left *</label>
                            <input type="number" id="stock" name="stock" min="0" value="10" required>
                        </div>

                        <div class="field-box">
                            <label for="threshold">Low Threshold *</label>
                            <input type="number" id="threshold" name="threshold" min="0" value="5" required>
                        </div>

                        <button type="submit" class="btn-primary">+ Add Product</button>
                    </form>
                </div>

                <!-- 2. Inventory Table with Category Filter & Stock Sort -->
                <div class="card">
                    <h2>Current Stock Inventory</h2>
                    <p class="subtext">Items at or below your threshold are highlighted in red. Update stock or change thresholds anytime.</p>

                    <!-- Filter & Sort Controls -->
                    <form action="DashboardServlet" method="GET" class="filter-bar">
                        <input type="hidden" name="tab" value="inventory">
                        <div class="filter-controls">
                            <label for="catFilter">Filter Category:</label>
                            <select id="catFilter" name="category" onchange="this.form.submit()">
                                <option value="ALL" <%= "ALL".equals(selectedCategory) ? "selected" : "" %>>All Categories</option>
                                <% if (categories != null) {
                                    for (String cat : categories) { %>
                                        <option value="<%= cat %>" <%= cat.equals(selectedCategory) ? "selected" : "" %>>
                                            <%= cat %>
                                        </option>
                                <%  }
                                } %>
                            </select>

                            <label for="sortFilter">Sort By:</label>
                            <select id="sortFilter" name="sort" onchange="this.form.submit()">
                                <option value="default" <%= "default".equals(selectedSort) ? "selected" : "" %>>Newest First</option>
                                <option value="stock_asc" <%= "stock_asc".equals(selectedSort) ? "selected" : "" %>>Stock: Small to Big</option>
                            </select>
                        </div>
                        <span class="subtext">
                            Showing <strong><%= inventoryList != null ? inventoryList.size() : 0 %></strong> item(s)
                        </span>
                    </form>

                    <% if (invError != null) { %>
                        <p style="color: #c53030; margin-top: 12px;"><%= invError %></p>
                    <% } else if (inventoryList != null && !inventoryList.isEmpty()) { %>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Product Name</th>
                                    <th>Category</th>
                                    <th>Status</th>
                                    <th>Stocks Left &amp; Threshold</th>
                                    <th>Remove</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (String[] item : inventoryList) {
                                    String id = item[0];
                                    String pName = item[1];
                                    String pCat = item[2];
                                    int stockLeft = Integer.parseInt(item[3]);
                                    int lowThresh = Integer.parseInt(item[4]);
                                    boolean isLow = stockLeft <= lowThresh;
                                %>
                                <tr class="<%= isLow ? "low-stock-row" : "" %>">
                                    <td><strong><%= pName %></strong></td>
                                    <td><span class="cat-badge"><%= pCat %></span></td>
                                    <td>
                                        <% if (isLow) { %>
                                            <span class="status-badge status-low">LOW STOCK</span>
                                        <% } else { %>
                                            <span class="status-badge status-ok">In Stock</span>
                                        <% } %>
                                    </td>
                                    <td>
                                        <form action="InventoryServlet" method="POST" style="display: flex; align-items: center; gap: 8px; margin: 0;">
                                            <input type="hidden" name="action" value="update">
                                            <input type="hidden" name="id" value="<%= id %>">
                                            <input type="hidden" name="filterCategory" value="<%= selectedCategory %>">
                                            <input type="hidden" name="sortStock" value="<%= selectedSort %>">

                                            <label style="font-size: 12px; color: #4a5568;">Stock:</label>
                                            <input type="number" name="stock" value="<%= stockLeft %>" min="0" class="num-input" required>

                                            <label style="font-size: 12px; color: #4a5568;">Alert &le;</label>
                                            <input type="number" name="threshold" value="<%= lowThresh %>" min="0" class="num-input" required>

                                            <button type="submit" class="btn-save">Update</button>
                                        </form>
                                    </td>
                                    <td>
                                        <form action="InventoryServlet" method="POST" style="margin: 0;"
                                              onsubmit="return confirm('Remove <%= pName %> from inventory?');">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="<%= id %>">
                                            <input type="hidden" name="filterCategory" value="<%= selectedCategory %>">
                                            <input type="hidden" name="sortStock" value="<%= selectedSort %>">
                                            <button type="submit" class="btn-danger">Remove</button>
                                        </form>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <p class="subtext" style="margin-top: 16px;">No products found. Use the form above to add your first item.</p>
                    <% } %>
                </div>

            <% } else if ("users".equals(activeTab) && isAdmin) { %>
                <!-- ADMIN: Manage Users Tab -->
                <div class="card admin-card">
                    <h2>Registered Users (Login Access)</h2>
                    <p class="subtext">Manage accounts authorized to sign in to the portal.</p>

                    <%
                        String dbError = (String) request.getAttribute("dbError");
                        List<String[]> userList = (List<String[]>) request.getAttribute("userList");
                        if (dbError != null) {
                    %>
                        <p style="color: #c53030; margin-top: 12px;"><%= dbError %></p>
                    <% } else if (userList != null && !userList.isEmpty()) { %>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>User ID</th>
                                    <th>Username</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (String[] u : userList) {
                                    boolean rowIsAdmin = "admin".equals(u[1]);
                                %>
                                <tr>
                                    <td>#<%= u[0] %></td>
                                    <td><strong><%= u[1] %></strong></td>
                                    <td>
                                        <span class="role-tag <%= rowIsAdmin ? "admin" : "" %>">
                                            <%= rowIsAdmin ? "Administrator" : "Standard User" %>
                                        </span>
                                    </td>
                                    <td style="color: #2f855a; font-weight: 600;">Active</td>
                                    <td>
                                        <% if (!rowIsAdmin) { %>
                                            <form action="DeleteUserServlet" method="POST" style="margin: 0;"
                                                  onsubmit="return confirm('Revoke login access for <%= u[1] %>?');">
                                                <input type="hidden" name="userId" value="<%= u[0] %>">
                                                <button type="submit" class="btn-danger">Revoke Access</button>
                                            </form>
                                        <% } else { %>
                                            <span class="protected-text">Protected</span>
                                        <% } %>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <p class="subtext" style="margin-top: 12px;">No users found in the database.</p>
                    <% } %>
                </div>

            <% } else if ("profile".equals(activeTab)) { %>
                <div class="card">
                    <h2>Account Profile</h2>
                    <p class="subtext" style="margin-top: 8px;">Username: <strong><%= currentUser %></strong></p>
                    <p class="subtext" style="margin-top: 4px;">Account Role: <strong><%= isAdmin ? "Administrator" : "Standard User" %></strong></p>
                </div>

            <% } else if ("reports".equals(activeTab)) { %>
                <div class="card">
                    <h2>System Reports</h2>
                    <p class="subtext" style="margin-top: 8px;">No recent reports generated yet.</p>
                </div>

            <% } else if ("settings".equals(activeTab)) { %>
                <div class="card">
                    <h2>Portal Settings</h2>
                    <p class="subtext" style="margin-top: 8px;">Application preferences and notification settings appear here.</p>
                </div>

            <% } else { %>
                <div class="grid">
                    <div class="card">
                        <h3>Welcome Back, <%= currentUser %></h3>
                        <p class="subtext">Use the sidebar on the left to navigate your workspace.</p>
                    </div>
                    <div class="card">
                        <h3>Inventory Control</h3>
                        <p class="subtext">Open <strong>Manage Inventory</strong> in the sidebar to add stock, set thresholds, and monitor low items.</p>
                    </div>
                    <% if (isAdmin) { %>
                    <div class="card admin-card">
                        <h3>Admin Access</h3>
                        <p class="subtext">Select <strong>Manage Users</strong> in the sidebar to view or revoke user login access.</p>
                    </div>
                    <% } %>
                </div>
            <% } %>
        </main>
    </div>
</body>
</html>
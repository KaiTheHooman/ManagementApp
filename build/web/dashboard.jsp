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
            max-width: 1160px;
            width: 100%;
        }

        /* Cards & Grids */
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
            margin-bottom: 24px;
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
        .card.warn-card {
            border-top-color: #dd6b20;
        }
        .card.danger-card {
            border-top-color: #e53e3e;
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
        .kpi-number {
            font-size: 32px;
            font-weight: 700;
            color: #1a202c;
            margin: 6px 0;
        }
        .subtext {
            color: #718096;
            font-size: 14px;
            line-height: 1.5;
        }

        /* Forms */
        .inv-form-grid {
            display: grid;
            grid-template-columns: 1.2fr 1.8fr 1.4fr 0.9fr 0.9fr auto;
            gap: 12px;
            align-items: end;
            margin-top: 16px;
        }
        .adj-form-grid {
            display: grid;
            grid-template-columns: 2fr 1.2fr 1fr auto;
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
        .field-box input, .field-box select, .filter-bar select {
            width: 100%;
            padding: 9px 11px;
            border: 1px solid #cbd5e0;
            border-radius: 6px;
            font-size: 14px;
            background-color: #ffffff;
        }
        .field-box input:focus, .field-box select:focus, .filter-bar select:focus {
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
        .btn-save {
            background-color: #38a169;
            color: #ffffff;
            border: none;
            padding: 7px 14px;
            border-radius: 5px;
            font-size: 13px;
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

        /* Alerts & Filter Bar */
        .alert-box {
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 18px;
            font-size: 14px;
            font-weight: 500;
        }
        .alert-success {
            background-color: #c6f6d5;
            color: #22543d;
            border: 1px solid #9ae6b4;
        }
        .alert-error {
            background-color: #fed7d7;
            color: #9b2c2c;
            border: 1px solid #feb2b2;
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
        }

        /* Tables & Badges */
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
        .pid-code {
            font-family: Consolas, monospace;
            background-color: #edf2f7;
            padding: 3px 7px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: 700;
            color: #2d3748;
        }
        .cat-badge {
            display: inline-block;
            padding: 3px 9px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
            background-color: #ebf8ff;
            color: #2b6cb0;
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
            width: 72px;
            padding: 6px 8px;
            border: 1px solid #cbd5e0;
            border-radius: 5px;
            font-size: 13px;
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
                <a href="DashboardServlet?tab=adjustment" class="<%= "adjustment".equals(activeTab) ? "active" : "" %>">
                    Stock Adjustment
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=summary" class="<%= "summary".equals(activeTab) ? "active" : "" %>">
                    Inventory Summary
                </a>
            </li>
            <li>
                <a href="DashboardServlet?tab=profile" class="<%= "profile".equals(activeTab) ? "active" : "" %>">
                    My Profile
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
                    Manage Inventory
                <% } else if ("adjustment".equals(activeTab)) { %>
                    Stock Adjustment (Cart)
                <% } else if ("summary".equals(activeTab)) { %>
                    Inventory Summary &amp; Tracking
                <% } else if ("users".equals(activeTab)) { %>
                    User Access Management
                <% } else if ("profile".equals(activeTab)) { %>
                    My Profile
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
                <!-- 1. Add New Product Card (With Product ID) -->
                <div class="card">
                    <h2>Add Product to Inventory</h2>
                    <p class="subtext">Enter a unique Product ID and required Category (automatically formatted in CAPS). Duplicate IDs or items merge stock automatically.</p>

                    <form action="InventoryServlet" method="POST" class="inv-form-grid">
                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="filterCategory" value="<%= selectedCategory %>">
                        <input type="hidden" name="sortStock" value="<%= selectedSort %>">

                        <div class="field-box">
                            <label for="productId">Product ID *</label>
                            <input type="text" id="productId" name="productId" placeholder="e.g. PRD-001" required>
                        </div>

                        <div class="field-box">
                            <label for="productName">Product Name *</label>
                            <input type="text" id="productName" name="productName" placeholder="e.g. Wireless Mouse" required>
                        </div>

                        <div class="field-box">
                            <label for="category">Category (Required) *</label>
                            <input type="text" id="category" name="category" list="categorySuggestions" placeholder="e.g. ELECTRONICS" style="text-transform: uppercase;" required>
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
                            <label for="threshold">Low Alert *</label>
                            <input type="number" id="threshold" name="threshold" min="0" value="5" required>
                        </div>

                        <button type="submit" class="btn-primary">+ Add Product</button>
                    </form>
                </div>

                <!-- 2. Inventory Table -->
                <div class="card">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px;">
                        <div>
                            <h2>Current Stock Inventory</h2>
                            <p class="subtext">Filter by category or sort stock from smallest to largest. Items at or below your threshold highlight in red.</p>
                        </div>

                        <% if (isAdmin && inventoryList != null && !inventoryList.isEmpty()) { %>
                            <form action="InventoryServlet" method="POST" style="margin: 0;"
                                  onsubmit="return confirm('WARNING: This will permanently delete ALL products from the inventory to start fresh. Continue?');">
                                <input type="hidden" name="action" value="clear_all">
                                <button type="submit" class="btn-danger" style="padding: 8px 14px; font-size: 13px;">
                                    Clear Whole Inventory
                                </button>
                            </form>
                        <% } %>
                    </div>

                    <form action="DashboardServlet" method="GET" class="filter-bar">
                        <input type="hidden" name="tab" value="inventory">
                        <div class="filter-controls">
                            <label for="catFilter">Filter Category:</label>
                            <select id="catFilter" name="category" onchange="this.form.submit()">
                                <option value="ALL" <%= "ALL".equals(selectedCategory) ? "selected" : "" %>>ALL CATEGORIES</option>
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
                                    <th>Product ID</th>
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
                                    String pId = item[1];
                                    String pName = item[2];
                                    String pCat = item[3];
                                    int stockLeft = Integer.parseInt(item[4]);
                                    int lowThresh = Integer.parseInt(item[5]);
                                    boolean isLow = stockLeft <= lowThresh;
                                %>
                                <tr class="<%= isLow ? "low-stock-row" : "" %>">
                                    <td><span class="pid-code"><%= pId %></span></td>
                                    <td><strong><%= pName %></strong></td>
                                    <td><span class="cat-badge"><%= pCat %></span></td>
                                    <td>
                                        <% if (stockLeft == 0) { %>
                                            <span class="status-badge status-low">EMPTY (0)</span>
                                        <% } else if (isLow) { %>
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
                                              onsubmit="return confirm('Remove <%= pName %> (<%= pId %>) from inventory?');">
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
                        <p class="subtext" style="margin-top: 16px;">No products found. Add your first item above.</p>
                    <% } %>
                </div>

            <% } else if ("adjustment".equals(activeTab)) {
                List<String[]> inventoryList = (List<String[]>) request.getAttribute("inventoryList");
                @SuppressWarnings("unchecked")
                List<String[]> adjCart = (List<String[]>) session.getAttribute("adjCart");
                String adjMessage = (String) session.getAttribute("adjMessage");
                String adjMsgType = (String) session.getAttribute("adjMsgType");
                if (adjMessage != null) {
                    session.removeAttribute("adjMessage");
                    session.removeAttribute("adjMsgType");
                }
            %>
                <% if (adjMessage != null) { %>
                    <div class="alert-box <%= "error".equals(adjMsgType) ? "alert-error" : "alert-success" %>">
                        <%= adjMessage %>
                    </div>
                <% } %>

                <!-- 1. Scan / Enter Product ID Card -->
                <div class="card">
                    <h2>Stock In / Stock Out Scanner</h2>
                    <p class="subtext">
                        Enter or scan a <strong>Product ID</strong> into the empty box below. Stock is updated immediately across the platform.
                        If an item shows 0 stock in the system during a Stock Out scan, the extra scanned units are automatically reconciled.
                    </p>

                    <form action="StockAdjustmentServlet" method="POST" class="adj-form-grid">
                        <input type="hidden" name="action" value="scan">

                        <div class="field-box">
                            <label for="scanProductId">Product ID (Empty Box) *</label>
                            <input type="text" id="scanProductId" name="productId" list="productIdSuggestions"
                                   placeholder="Scan or type Product ID..." autofocus required>
                            <datalist id="productIdSuggestions">
                                <% if (inventoryList != null) {
                                    for (String[] item : inventoryList) { %>
                                        <option value="<%= item[1] %>"><%= item[2] %> (Stock: <%= item[4] %>)</option>
                                <%  }
                                } %>
                            </datalist>
                        </div>

                        <div class="field-box">
                            <label for="mode">Adjustment Type *</label>
                            <select id="mode" name="mode">
                                <option value="OUT">STOCK OUT (- Deduct)</option>
                                <option value="IN">STOCK IN (+ Add)</option>
                            </select>
                        </div>

                        <div class="field-box">
                            <label for="qty">Quantity *</label>
                            <input type="number" id="qty" name="qty" min="1" value="1" required>
                        </div>

                        <button type="submit" class="btn-primary">+ Add to Cart</button>
                    </form>
                </div>

                <!-- 2. Personal Adjustment Cart -->
                <div class="card admin-card">
                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                        <div>
                            <h2>Your Active Adjustment Cart (<%= currentUser %>)</h2>
                            <p class="subtext">Items listed here have already been applied to live platform stock. Undo an entry or confirm when done.</p>
                        </div>
                        <% if (adjCart != null && !adjCart.isEmpty()) { %>
                            <form action="StockAdjustmentServlet" method="POST" style="margin: 0;">
                                <input type="hidden" name="action" value="finalize">
                                <button type="submit" class="btn-save">Confirm &amp; Clear Cart</button>
                            </form>
                        <% } %>
                    </div>

                    <% if (adjCart != null && !adjCart.isEmpty()) { %>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Product ID</th>
                                    <th>Product Name</th>
                                    <th>Type</th>
                                    <th>Qty</th>
                                    <th>System Action Note</th>
                                    <th>Undo</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (String[] cItem : adjCart) {
                                    boolean isOut = "OUT".equals(cItem[3]);
                                %>
                                <tr>
                                    <td><span class="pid-code"><%= cItem[1] %></span></td>
                                    <td><strong><%= cItem[2] %></strong></td>
                                    <td>
                                        <span class="status-badge <%= isOut ? "status-low" : "status-ok" %>">
                                            <%= isOut ? "STOCK OUT" : "STOCK IN" %>
                                        </span>
                                    </td>
                                    <td><strong><%= isOut ? "-" : "+" %><%= cItem[4] %></strong></td>
                                    <td class="subtext"><%= cItem[5] %></td>
                                    <td>
                                        <form action="StockAdjustmentServlet" method="POST" style="margin: 0;">
                                            <input type="hidden" name="action" value="remove_item">
                                            <input type="hidden" name="entryId" value="<%= cItem[0] %>">
                                            <button type="submit" class="btn-danger">Undo</button>
                                        </form>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <p class="subtext" style="margin-top: 16px;">Your cart is empty. Scan or enter a Product ID above to begin.</p>
                    <% } %>
                </div>

            <% } else if ("summary".equals(activeTab)) {
                Integer totalProducts = (Integer) request.getAttribute("totalProducts");
                Integer totalUnits = (Integer) request.getAttribute("totalUnits");
                Integer lowStockCount = (Integer) request.getAttribute("lowStockCount");
                Integer outOfStockCount = (Integer) request.getAttribute("outOfStockCount");
                List<String[]> categorySummary = (List<String[]>) request.getAttribute("categorySummary");
                List<String[]> stockLogs = (List<String[]>) request.getAttribute("stockLogs");
                String summaryError = (String) request.getAttribute("summaryError");
            %>
                <% if (summaryError != null) { %>
                    <div class="alert-box alert-error"><%= summaryError %></div>
                <% } %>

                <!-- 1. KPI Summary Cards -->
                <div class="grid">
                    <div class="card">
                        <span class="subtext">Total Unique Products</span>
                        <div class="kpi-number"><%= totalProducts != null ? totalProducts : 0 %></div>
                        <p class="subtext">Registered Product IDs</p>
                    </div>
                    <div class="card admin-card">
                        <span class="subtext">Total Units in Stock</span>
                        <div class="kpi-number"><%= totalUnits != null ? totalUnits : 0 %></div>
                        <p class="subtext">Combined available items</p>
                    </div>
                    <div class="card warn-card">
                        <span class="subtext">Low Stock Alerts</span>
                        <div class="kpi-number" style="color: #dd6b20;"><%= lowStockCount != null ? lowStockCount : 0 %></div>
                        <p class="subtext">At or below threshold</p>
                    </div>
                    <div class="card danger-card">
                        <span class="subtext">Out of Stock</span>
                        <div class="kpi-number" style="color: #e53e3e;"><%= outOfStockCount != null ? outOfStockCount : 0 %></div>
                        <p class="subtext">Items currently at 0</p>
                    </div>
                </div>

                <!-- 2. Stock Summary by Category -->
                <div class="card">
                    <h2>Stock Summary by Category</h2>
                    <p class="subtext">Breakdown of total products and stock volume per category.</p>

                    <% if (categorySummary != null && !categorySummary.isEmpty()) { %>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Category</th>
                                    <th>Unique Products</th>
                                    <th>Total Stock Units</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (String[] cs : categorySummary) { %>
                                <tr>
                                    <td><span class="cat-badge"><%= cs[0] %></span></td>
                                    <td><%= cs[1] %> product(s)</td>
                                    <td><strong><%= cs[2] %> units</strong></td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <p class="subtext" style="margin-top: 12px;">No inventory data available yet.</p>
                    <% } %>
                </div>

                <!-- 3. Stock Movement & Audit Tracking Log -->
                <div class="card">
                    <h2>Recent Stock Movement &amp; Discrepancy Log</h2>
                    <p class="subtext">Tracks every Stock In, Stock Out, Manual Update, and Scan Discrepancy Fix across all users.</p>

                    <% if (stockLogs != null && !stockLogs.isEmpty()) { %>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Product ID</th>
                                    <th>Product Name</th>
                                    <th>Action</th>
                                    <th>Qty Changed</th>
                                    <th>Stock After</th>
                                    <th>User</th>
                                    <th>Timestamp</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (String[] log : stockLogs) {
                                    String act = log[2];
                                    boolean isNegative = act.contains("OUT") || act.contains("REMOVED");
                                %>
                                <tr>
                                    <td><span class="pid-code"><%= log[0] %></span></td>
                                    <td><strong><%= log[1] %></strong></td>
                                    <td>
                                        <span class="status-badge <%= isNegative ? "status-low" : "status-ok" %>">
                                            <%= act %>
                                        </span>
                                    </td>
                                    <td><strong><%= isNegative ? "-" : "+" %><%= log[3] %></strong></td>
                                    <td><%= log[4] %></td>
                                    <td><%= log[5] %></td>
                                    <td class="subtext"><%= log[6] %></td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <p class="subtext" style="margin-top: 12px;">No stock movements recorded yet.</p>
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

            <% } else { %>
                <div class="grid">
                    <div class="card">
                        <h3>Welcome Back, <%= currentUser %></h3>
                        <p class="subtext">Use the sticky sidebar on the left to navigate your workspace.</p>
                    </div>
                    <div class="card">
                        <h3>Manage Inventory</h3>
                        <p class="subtext">Add products with unique Product IDs, CAPS categories, and custom low-stock alerts.</p>
                    </div>
                    <div class="card admin-card">
                        <h3>Stock Adjustment</h3>
                        <p class="subtext">Scan Product IDs to Stock In or Stock Out with automatic live deduction and scan discrepancy recovery.</p>
                    </div>
                    <div class="card warn-card">
                        <h3>Inventory Summary</h3>
                        <p class="subtext">View total stock metrics, category totals, and complete movement audit history.</p>
                    </div>
                </div>
            <% } %>
        </main>
    </div>
</body>
</html>
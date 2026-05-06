<%--
  @author: wangyue
  @Since: 2026/4/16
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>Create MO Account</title>
    <link rel="stylesheet" href="static/css/admin.css">
    <script src="script/jquery-3.6.0.min.js"></script>
</head>
<body>

<header class="navbar">
    <div class="container">
        <div class="logo">ADMIN</div>

        <div class="nav-right">
            <div class="user-info-wrapper">
                <div class="user-info">
                    <img src="static/img/user.png" alt="User Avatar">
                    <span class="username">${sessionScope.user.name}</span>
                </div>

                <div class="user-dropdown">
                    <a class="dropdown-item exit">Exit</a>
                </div>
            </div>

            <nav class="nav-menu">
                <a href="adminServlet?action=listAccounts&filter=all&order=name" class="nav-item">Home</a>
                <a href="views/admin/post.jsp" class="nav-item active">Post</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">Create MO Account</h1>

            <form id="createMoForm" class="account-form">
                <div class="form-section">
                    <h3 class="section-title">Account Information</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="username">Username <span class="required">*</span></label>
                            <input type="text" id="username" name="username" class="form-control"
                                   placeholder="Enter username" required>
                        </div>

                        <div class="form-group">
                            <label for="password">Password <span class="required">*</span></label>
                            <input type="password" id="password" name="password" class="form-control"
                                   placeholder="Minimum 6 characters" required minlength="6">
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Profile Information</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="name">Full Name <span class="required">*</span></label>
                            <input type="text" id="name" name="name" class="form-control"
                                   placeholder="Enter full name" required>
                        </div>

                        <div class="form-group">
                            <label for="college">College</label>
                            <input type="text" id="college" name="college" class="form-control"
                                   placeholder="Enter college name">
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" class="form-control"
                                   placeholder="Enter email address">
                        </div>

                        <div class="form-group">
                            <label for="phone">Phone</label>
                            <input type="tel" id="phone" name="phone" class="form-control"
                                   placeholder="Enter phone number">
                        </div>
                    </div>
                </div>

                <div id="formError" class="alert alert-danger" style="display: none;"></div>
                <div id="formSuccess" class="alert alert-success" style="display: none;"></div>

                <div class="form-actions">
                    <a href="adminServlet?action=listAccounts" class="btn btn-secondary">Cancel</a>
                    <button type="submit" class="btn btn-primary">Create MO Account</button>
                </div>
            </form>
        </div>
    </div>
</main>

<div id="messageModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3 id="messageModalTitle">Notice</h3>
        <p id="messageModalBody">Message content...</p>
        <button class="btn-confirm" id="confirmMessageModal">OK</button>
    </div>
</div>

<script src="static/js/admin.js"></script>
</body>
</html>

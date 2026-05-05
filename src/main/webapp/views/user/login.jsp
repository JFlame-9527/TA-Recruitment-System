<%--
  @author: Jflame
  @Since: 2026/3/23
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>Login</title>
    <link rel="stylesheet" href="static/css/user.css">
    <script src="script/jquery-3.6.0.min.js"></script>
</head>
<body>

<header class="navbar">
    <div class="container">
        <div class="logo">TARS</div>
        <div class="nav-placeholder"></div>
    </div>
</header>

<main class="main-content">
    <div class="container">
        <div class="login-wrapper">
            <div class="login-box">
                <h2 class="login-title">Welcome</h2>

                <form id="loginForm" action="userServlet" method="post">
                    <input type="hidden" name="action" value="login">

                    <div class="form-group">
                        <label for="username" class="sr-only">username</label>
                        <input type="text" id="username" name="username" class="form-control" placeholder="username"
                               value="${username}" required>
                    </div>

                    <div class="form-group">
                        <label for="password" class="sr-only">password</label>
                        <input type="password" id="password" name="password" class="form-control"
                               placeholder="password" required>
                    </div>

                    <c:if test="${not empty error}">
                        <div class="alert alert-danger">
                            <span>${error}</span>
                        </div>
                    </c:if>

                    <div class="form-options">
                        <a href="#" class="link-text" id="forgotPwd">forgot password?</a>
                        <a href="#" class="link-text" id="registerLink">register</a>
                    </div>

                    <button type="submit" class="btn-login" id="btn-login">LOGIN</button>
                </form>
            </div>
        </div>
    </div>
</main>

<div id="registerModal" class="modal">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3 id="registerTitle">Register Account</h3>
        <form id="registerForm">
            <div class="form-group">
                <input type="text" id="reg_username" name="username" class="form-control" placeholder="Username" required>
            </div>
            <div class="form-group">
                <input type="password" id="reg_password" name="password" class="form-control" placeholder="Password" required>
            </div>
            <div class="form-group">
                <input type="password" id="reg_checkpassword" name="checkpassword" class="form-control" placeholder="Confirm Password" required>
            </div>
            <div id="registerError" class="alert alert-danger" style="display: none;"></div>
            <button type="submit" class="btn-login" id="btn-register">Register</button>
        </form>
    </div>
</div>

<div id="messageModal" class="modal">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3 id="modalTitle">Notice</h3>
        <p id="modalBody">Loading...</p>
        <button class="btn-confirm" id="confirmModal">confirm</button>
    </div>
</div>

<script src="static/js/user.js"></script>
</body>
</html>

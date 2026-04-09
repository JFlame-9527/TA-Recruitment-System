<%--
  @author: Yue Wang
  @Since: 2026/3/24
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>Admin - User Management</title>
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
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">User Management</h1>

            <div class="tab-navigation">
                <button class="tab-btn active" data-role="1" onclick="switchTab(1)">TA Accounts</button>
                <button class="tab-btn" data-role="2" onclick="switchTab(2)">MO Accounts</button>
            </div>

            <div id="taListContainer" class="user-list-container active">
                <c:choose>
                    <c:when test="${empty taList}">
                        <div class="empty-state">
                            <div class="empty-icon">👥</div>
                            <div class="empty-text">No TA accounts found</div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="user-list">
                            <c:forEach var="user" items="${taList}">
                                <div class="user-card" data-userid="${user.userId}" data-role="1">
                                    <div class="user-info-section">
                                        <c:set var="shortId" value="${fn:length(user.userId) > 8 ? fn:substring(user.userId, 0, 8) : user.userId}"/>
                                        <span class="user-id">#${shortId}</span>
                                        <span class="username">${user.name}</span>
                                        <span class="role-badge role-ta">TA</span>
                                        <span class="status-badge ${user.status == 0 ? 'status-available' : 'status-frozen'}">
                                            ${user.status == 0 ? 'Available' : 'Frozen'}
                                        </span>
                                        <span class="time-info">
                                            Created: <fmt:formatDate value="${user.createAt}" pattern="yyyy-MM-dd HH:mm"/>
                                        </span>
                                    </div>

                                    <div class="user-actions">
                                        <button class="btn btn-view" onclick="viewProfile('${user.userId}', 1)">View Profile</button>
                                        <button class="btn btn-toggle" onclick="toggleStatus('${user.userId}', ${user.status})">
                                            ${user.status == 0 ? 'Disable' : 'Enable'}
                                        </button>
                                        <button class="btn btn-edit" onclick="editUser('${user.userId}', 1)">Edit</button>
                                        <button class="btn btn-delete" onclick="deleteUser('${user.userId}')">Delete</button>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>

                        <div class="pagination" data-role="1">
                            <c:if test="${taCurrentPage > 1}">
                                <a class="page-item" data-page="${taCurrentPage - 1}">&laquo;</a>
                            </c:if>

                            <c:forEach begin="1" end="${taTotalPages}" var="i">
                                <a class="page-item ${taCurrentPage == i ? 'active' : ''}"
                                   data-page="${i}">${i}</a>
                            </c:forEach>

                            <c:if test="${taCurrentPage < taTotalPages}">
                                <a class="page-item" data-page="${taCurrentPage + 1}">&raquo;</a>
                            </c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div id="moListContainer" class="user-list-container" style="display: none;">
                <c:choose>
                    <c:when test="${empty moList}">
                        <div class="empty-state">
                            <div class="empty-icon">👥</div>
                            <div class="empty-text">No MO accounts found</div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="user-list">
                            <c:forEach var="user" items="${moList}">
                                <div class="user-card" data-userid="${user.userId}" data-role="2">
                                    <div class="user-info-section">
                                        <c:set var="shortId" value="${fn:length(user.userId) > 8 ? fn:substring(user.userId, 0, 8) : user.userId}"/>
                                        <span class="user-id">#${shortId}</span>
                                        <span class="username">${user.name}</span>
                                        <span class="role-badge role-mo">MO</span>
                                        <span class="status-badge ${user.status == 0 ? 'status-available' : 'status-frozen'}">
                                            ${user.status == 0 ? 'Available' : 'Frozen'}
                                        </span>
                                        <span class="time-info">
                                            Created: <fmt:formatDate value="${user.createAt}" pattern="yyyy-MM-dd HH:mm"/>
                                        </span>
                                    </div>

                                    <div class="user-actions">
                                        <button class="btn btn-view" onclick="viewProfile('${user.userId}', 2)">View Profile</button>
                                        <button class="btn btn-toggle" onclick="toggleStatus('${user.userId}', ${user.status})">
                                            ${user.status == 0 ? 'Disable' : 'Enable'}
                                        </button>
                                        <button class="btn btn-edit" onclick="editUser('${user.userId}', 2)">Edit</button>
                                        <button class="btn btn-delete" onclick="deleteUser('${user.userId}')">Delete</button>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>

                        <div class="pagination" data-role="2">
                            <c:if test="${moCurrentPage > 1}">
                                <a class="page-item" data-page="${moCurrentPage - 1}">&laquo;</a>
                            </c:if>

                            <c:forEach begin="1" end="${moTotalPages}" var="i">
                                <a class="page-item ${moCurrentPage == i ? 'active' : ''}"
                                   data-page="${i}">${i}</a>
                            </c:forEach>

                            <c:if test="${moCurrentPage < moTotalPages}">
                                <a class="page-item" data-page="${moCurrentPage + 1}">&raquo;</a>
                            </c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>

<div id="viewTaProfileModal" class="modal" style="display: none;">
    <div class="modal-content modal-large">
        <span class="close-modal">&times;</span>
        <h3 id="taProfileTitle">TA Profile</h3>
        <div id="taProfileContent"></div>
    </div>
</div>

<div id="viewMoProfileModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3 id="moProfileTitle">MO Profile</h3>
        <div id="moProfileContent"></div>
    </div>
</div>

<div id="editTaUserModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3>Edit TA Account</h3>
        <form id="editTaForm">
            <input type="hidden" name="userId" id="editTaUserId">

            <div class="form-group">
                <label for="editTaUsername">Username</label>
                <input type="text" id="editTaUsername" name="username" class="form-control" required>
            </div>

            <div class="form-group">
                <label for="editTaPassword">New Password (min 6 characters)</label>
                <input type="password" id="editTaPassword" name="newPassword" class="form-control" minlength="6">
                <small class="form-hint">Leave empty to keep current password</small>
            </div>

            <div id="editTaError" class="alert alert-danger" style="display: none;"></div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editTaUserModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save</button>
            </div>
        </form>
    </div>
</div>

<div id="editMoAccountModal" class="modal" style="display: none;">
    <div class="modal-content modal-large">
        <span class="close-modal">&times;</span>
        <h3>Edit MO Account</h3>
        <form id="editMoForm">
            <input type="hidden" name="userId" id="editMoUserId">
            <input type="hidden" name="profileId" id="editMoProfileId">

            <div class="form-section">
                <h4 class="section-title">User Information</h4>
                <div class="form-group">
                    <label for="editMoUsername">Username</label>
                    <input type="text" id="editMoUsername" name="username" class="form-control" required>
                </div>

                <div class="form-group">
                    <label for="editMoPassword">New Password (min 6 characters)</label>
                    <input type="password" id="editMoPassword" name="newPassword" class="form-control" minlength="6">
                    <small class="form-hint">Leave empty to keep current password</small>
                </div>
            </div>

            <div class="form-section">
                <h4 class="section-title">Profile Information</h4>
                <div class="form-row">
                    <div class="form-group">
                        <label for="editMoName">Name</label>
                        <input type="text" id="editMoName" name="name" class="form-control">
                    </div>

                    <div class="form-group">
                        <label for="editMoCollege">College</label>
                        <input type="text" id="editMoCollege" name="college" class="form-control">
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="editMoEmail">Email</label>
                        <input type="email" id="editMoEmail" name="email" class="form-control">
                    </div>

                    <div class="form-group">
                        <label for="editMoPhone">Phone</label>
                        <input type="tel" id="editMoPhone" name="phone" class="form-control">
                    </div>
                </div>
            </div>

            <div id="editMoError" class="alert alert-danger" style="display: none;"></div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editMoAccountModal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save</button>
            </div>
        </form>
    </div>
</div>

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

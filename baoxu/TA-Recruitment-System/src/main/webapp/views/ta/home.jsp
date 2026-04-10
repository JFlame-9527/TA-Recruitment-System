<%--
  @author: Xiri04
  @Since: 2026/3/24
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>TA Home</title>
    <link rel="stylesheet" href="static/css/ta.css">
    <script src="script/jquery-3.6.0.min.js"></script>
</head>
<body>

<header class="navbar">
    <div class="container">
        <div class="logo">TA</div>

        <div class="nav-right">
            <div class="user-info-wrapper">
                <div class="user-info">
                    <img src="static/img/user.png" alt="User Avatar">
                    <span class="username">${sessionScope.user.name}</span>
                </div>

                <div class="user-dropdown">
                    <a class="dropdown-item edit">Edit</a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item exit">Exit</a>
                </div>
            </div>

            <nav class="nav-menu">
                <a href="taServlet?action=listApplied&page=1" class="nav-item active">Home</a>
                <a href="taServlet?action=listPositions&page=1" class="nav-item">Position</a>
                <a href="taServlet?action=getProfile" class="nav-item">Profile</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">My Applications</h1>

            <c:choose>
                <c:when test="${empty appliedList}">
                    <div class="empty-state">
                        <div class="empty-icon">📋</div>
                        <div class="empty-text">No applications found</div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="applied-list">
                        <c:forEach var="item" items="${appliedList}">
                            <div class="applied-card" id="card-${item.appId}">
                                <div class="card-header">
                                    <h3 class="card-title">${item.title}</h3>
                                    <p class="card-module">${item.moduleCode} - ${item.moduleName}</p>
                                </div>

                                <div class="card-stats">
                                    <span class="stat-label">Offered:</span>
                                    <span class="stat-value">${item.offeredNum}/${item.requiredNum}</span>
                                </div>

                                <div class="card-status">
                                    <span class="status-badge
                                        <c:choose>
                                            <c:when test="${item.status == 0}">status-applied</c:when>
                                            <c:when test="${item.status == 1}">status-offered</c:when>
                                            <c:when test="${item.status == 2}">status-rejected</c:when>
                                            <c:when test="${item.status == 3}">status-withdrawn</c:when>
                                        </c:choose>">
                                        <c:choose>
                                            <c:when test="${item.status == 0}">APPLIED</c:when>
                                            <c:when test="${item.status == 1}">OFFERED</c:when>
                                            <c:when test="${item.status == 2}">REJECTED</c:when>
                                            <c:when test="${item.status == 3}">WITHDRAWN</c:when>
                                        </c:choose>
                                    </span>
                                </div>

                                <div class="card-actions">
                                    <button class="btn btn-view"
                                            onclick="viewApplication('${item.appId}', '${item.posId}', ${currentPage})">
                                        View
                                    </button>
                                    <button class="btn btn-withdraw"
                                            data-appid="${item.appId}"
                                            <c:if test="${item.status != 0}">disabled</c:if>>
                                        <c:choose>
                                            <c:when test="${item.status == 0}">Withdraw</c:when>
                                            <c:otherwise>Withdrawn</c:otherwise>
                                        </c:choose>
                                    </button>
                                </div>
                            </div>
                        </c:forEach>
                    </div>

                    <div class="pagination">
                        <c:if test="${currentPage > 1}">
                            <a class="page-item" data-page="${currentPage - 1}">«</a>
                        </c:if>

                        <c:forEach begin="1" end="${totalPages}" var="i">
                            <a class="page-item ${currentPage == i ? 'active' : ''}"
                               data-page="${i}">${i}</a>
                        </c:forEach>

                        <c:if test="${currentPage < totalPages}">
                            <a class="page-item" data-page="${currentPage + 1}">»</a>
                        </c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<div id="messageModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-modal">&times;</span>
        <h3 id="modalTitle">Notice</h3>
        <p id="modalBody">Loading...</p>
        <button class="btn-confirm" id="confirmModal">Confirm</button>
    </div>
</div>

<script>
    window.pageLoading = false;
</script>

<script src="static/js/ta.js"></script>
</body>
</html>

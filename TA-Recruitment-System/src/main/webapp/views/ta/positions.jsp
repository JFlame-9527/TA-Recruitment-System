<%--
  @author: Xiri04
  @Since: 2026/4/4
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>Positions</title>
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
                <a href="taServlet?action=listApplied&page=1" class="nav-item">Home</a>
                <a href="taServlet?action=listPositions&page=1" class="nav-item active">Position</a>
                <a href="taServlet?action=getProfile" class="nav-item">Profile</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">Available Positions</h1>

            <div class="search-container">
                <div class="search-box">
                    <input type="text" class="search-input" placeholder="Search positions by title or module code..." id="searchInput">
                    <button class="search-btn">Search</button>
                </div>
            </div>

            <c:choose>
                <c:when test="${empty positionList}">
                    <div class="empty-state">
                        <div class="empty-icon">📋</div>
                        <div class="empty-text">No positions found</div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="position-list">
                        <c:forEach var="pos" items="${positionList}">
                            <div class="position-card" id="pos-${pos.posId}">
                                <div class="position-header">
                                    <h3 class="position-title">${pos.title}</h3>
                                    <p class="position-module">${pos.moduleCode} - ${pos.moduleName}</p>
                                </div>

                                <div class="position-details">
                                    <div class="detail-item">
                                        <span class="detail-label">Weekly Workload</span>
                                        <span class="detail-value">${pos.weeklyWorkload} hrs</span>
                                    </div>
                                    <div class="detail-item">
                                        <span class="detail-label">Duration</span>
                                        <span class="detail-value">${pos.duration} weeks</span>
                                    </div>
                                    <div class="detail-item">
                                        <span class="detail-label">Required</span>
                                        <span class="detail-value">${pos.requiredNum}</span>
                                    </div>
                                </div>

                                <div class="position-footer">
                                    <div class="status-group">
                                        <c:if test="${pos.appStatus != -1 || pos.appId != null}">
                                            <span class="status-badge
                                                <c:choose>
                                                    <c:when test="${pos.appStatus == 0}">status-applied</c:when>
                                                    <c:when test="${pos.appStatus == 1}">status-offered</c:when>
                                                    <c:when test="${pos.appStatus == 2}">status-rejected</c:when>
                                                    <c:when test="${pos.appStatus == 3}">status-withdrawn</c:when>
                                                </c:choose>">
                                                <c:choose>
                                                    <c:when test="${pos.appStatus == 0}">APPLIED</c:when>
                                                    <c:when test="${pos.appStatus == 1}">OFFERED</c:when>
                                                    <c:when test="${pos.appStatus == 2}">REJECTED</c:when>
                                                    <c:when test="${pos.appStatus == 3}">WITHDRAWN</c:when>
                                                </c:choose>
                                            </span>
                                        </c:if>

                                        <span class="status-badge
                                            <c:choose>
                                                <c:when test="${pos.posStatus == 0}">status-opened</c:when>
                                                <c:when test="${pos.posStatus == 1}">status-filled</c:when>
                                                <c:when test="${pos.posStatus == 2}">status-closed</c:when>
                                            </c:choose>">
                                            <c:choose>
                                                <c:when test="${pos.posStatus == 0}">OPENED</c:when>
                                                <c:when test="${pos.posStatus == 1}">FILLED</c:when>
                                                <c:when test="${pos.posStatus == 2}">CLOSED</c:when>
                                            </c:choose>
                                        </span>
                                    </div>

                                    <div class="date-info">
                                        <span class="deadline">Deadline: <fmt:formatDate value="${pos.deadline}" pattern="yyyy-MM-dd"/></span>
                                        <span style="margin-left: 15px;">Posted: <fmt:formatDate value="${pos.postDate}" pattern="yyyy-MM-dd"/></span>
                                    </div>
                                </div>

                                <div class="card-actions">
                                    <button class="btn-view-position"
                                            onclick="viewPosition('${pos.posId}', '${pos.appId != null ? pos.appId : ''}', ${currentPage})">
                                        View Details
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

<script src="static/js/ta.js"></script>
</body>
</html>

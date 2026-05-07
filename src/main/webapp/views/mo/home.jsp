<%--
  @author: Jflame
  @Since: 2026/4/5
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>MO Home</title>
    <link rel="stylesheet" href="static/css/mo.css">
    <script src="script/jquery-3.6.0.min.js"></script>
</head>
<body>

<header class="navbar">
    <div class="container">
        <div class="logo">MO</div>

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
                <a href="moServlet?action=listPosition&page=1&filter=all&order=postDate" class="nav-item active">Home</a>
                <a href="moServlet?action=postPosition" class="nav-item">Post Position</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <div class="page-header">
                <h1 class="page-title">My Positions</h1>

                <div class="page-controls">
                    <div class="control-item">
                        <label for="filterSelect">Filter:</label>
                        <select id="filterSelect" class="control-select" onchange="setHomeFilter(this.value)">
                            <option value="all" ${condition.filter == 'all' || empty condition.filter ? 'selected' : ''}>All</option>
                            <option value="opened" ${condition.filter == 'opened' ? 'selected' : ''}>Opened</option>
                            <option value="closed" ${condition.filter == 'closed' ? 'selected' : ''}>Closed</option>
                            <option value="filled" ${condition.filter == 'filled' ? 'selected' : ''}>Filled</option>
                            <option value="withdrawn" ${condition.filter == 'withdrawn' ? 'selected' : ''}>Withdrawn</option>
                        </select>
                    </div>
                    <div class="control-item">
                        <label for="orderSelect">Sort by:</label>
                        <select id="orderSelect" class="control-select" onchange="setHomeOrder(this.value)">
                            <option value="postDate" ${condition.order == 'postDate' || empty condition.order ? 'selected' : ''}>Post Date</option>
                            <option value="deadline" ${condition.order == 'deadline' ? 'selected' : ''}>Deadline</option>
                        </select>
                    </div>
                </div>
            </div>

            <c:choose>
                <c:when test="${empty positionList}">
                    <div class="empty-state">
                        <div class="empty-icon">📋</div>
                        <div class="empty-text">No positions found</div>
                        <a href="moServlet?action=postPosition" class="btn-create-first">Create Your First Position</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="position-list">
                        <c:forEach var="pos" items="${positionList}">
                            <div class="position-card">
                                <div class="card-header">
                                    <h3 class="card-title">${pos.title}</h3>
                                    <p class="card-module">${pos.moduleCode} - ${pos.moduleName}</p>
                                </div>

                                <div class="card-stats">
                                    <div class="stat-item">
                                        <span class="stat-label">Vacancy:</span>
                                        <span class="stat-value">${pos.vacancyNum}</span>
                                    </div>
                                    <div class="stat-item">
                                        <span class="stat-label">Pending:</span>
                                        <span class="stat-value pending">${pos.pendingNum}</span>
                                    </div>
                                </div>

                                <div class="card-dates">
                                    <div class="date-item">
                                        <span class="date-label">Posted:</span>
                                        <span class="date-value"><fmt:formatDate value="${pos.postDate}" pattern="yyyy-MM-dd"/></span>
                                    </div>
                                    <div class="date-item">
                                        <span class="date-label">Deadline:</span>
                                        <span class="date-value deadline"><fmt:formatDate value="${pos.deadline}" pattern="yyyy-MM-dd"/></span>
                                    </div>
                                </div>

                                <div class="card-status">
                                    <span class="status-badge
                                        <c:choose>
                                            <c:when test="${pos.status == 0}">status-opened</c:when>
                                            <c:when test="${pos.status == 1}">status-filled</c:when>
                                            <c:when test="${pos.status == 2}">status-closed</c:when>
                                            <c:when test="${pos.status == 3}">status-withdrawn</c:when>
                                        </c:choose>">
                                        <c:choose>
                                            <c:when test="${pos.status == 0}">OPENED</c:when>
                                            <c:when test="${pos.status == 1}">FILLED</c:when>
                                            <c:when test="${pos.status == 2}">CLOSED</c:when>
                                            <c:when test="${pos.status == 3}">WITHDRAWN</c:when>
                                        </c:choose>
                                    </span>
                                </div>

                                <div class="card-actions">
                                    <button class="btn btn-view"
                                            onclick="viewPosition('${pos.posId}', ${condition.page})">
                                        View Details
                                    </button>
                                </div>
                            </div>
                        </c:forEach>
                    </div>

                    <div class="pagination">
                        <c:if test="${condition.page > 1}">
                            <a class="page-item" data-page="${condition.page - 1}">&laquo;</a>
                        </c:if>

                        <c:forEach begin="1" end="${totalPages}" var="i">
                            <a class="page-item ${condition.page == i ? 'active' : ''}"
                               data-page="${i}">${i}</a>
                        </c:forEach>

                        <c:if test="${condition.page < totalPages}">
                            <a class="page-item" data-page="${condition.page + 1}">&raquo;</a>
                        </c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<script>
    window.pageLoading = false;
    let currentFilter = '${not empty condition.filter ? condition.filter : "all"}';
    let currentOrder = '${not empty condition.order ? condition.order : "postDate"}';
</script>

<script src="static/js/mo.js"></script>
</body>
</html>

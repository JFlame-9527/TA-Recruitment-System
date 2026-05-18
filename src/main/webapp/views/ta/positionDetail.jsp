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
    <title>Position Detail</title>
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
                <a href="taServlet?action=listApplied&page=1&filter=all&order=applyAt" class="nav-item">Home</a>
                <a href="taServlet?action=listPositions&page=1&filter=all&order=postDate" class="nav-item active">Position</a>
                <a href="taServlet?action=getProfile" class="nav-item">Profile</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <div class="detail-header">
                <button class="btn-back" onclick="goBack('${from}', '${page}')">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M19 12H5M12 19l-7-7 7-7"/>
                    </svg>
                    Back
                </button>
            </div>

            <div class="position-detail">
                <h1 class="detail-title">${position.title}</h1>
                <p class="detail-module">${position.moduleCode} - ${position.moduleName}</p>

                <div class="detail-section">
                    <h3 class="section-title">Description</h3>
                    <p class="section-content">${position.description}</p>
                </div>

                <div class="detail-section">
                    <h3 class="section-title">Required Skills</h3>
                    <div class="skills-list">
                        <c:forEach var="skill" items="${position.skills}">
                            <span class="skill-tag">${skill}</span>
                        </c:forEach>
                    </div>
                </div>

                <div class="detail-grid">
                    <div class="grid-item">
                        <span class="grid-label">Weekly Workload</span>
                        <span class="grid-value">${position.weeklyWorkload} hrs</span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">Duration</span>
                        <span class="grid-value">${position.duration} weeks</span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">Required</span>
                        <span class="grid-value">${position.requiredNum}</span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">Offered</span>
                        <span class="grid-value">${position.offeredNum}</span>
                    </div>
                </div>

                <div class="detail-grid">
                    <div class="grid-item">
                        <span class="grid-label">Start Date</span>
                        <span class="grid-value"><fmt:formatDate value="${position.startDate}" pattern="yyyy-MM-dd"/></span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">End Date</span>
                        <span class="grid-value"><fmt:formatDate value="${position.endDate}" pattern="yyyy-MM-dd"/></span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">Deadline</span>
                        <span class="grid-value"><fmt:formatDate value="${position.deadline}" pattern="yyyy-MM-dd"/></span>
                    </div>
                    <div class="grid-item">
                        <span class="grid-label">Post Date</span>
                        <span class="grid-value"><fmt:formatDate value="${position.postDate}" pattern="yyyy-MM-dd"/></span>
                    </div>
                </div>

                <div class="detail-section">
                    <h3 class="section-title">Position Status</h3>
                    <span class="status-badge
                        <c:choose>
                            <c:when test="${position.posStatus == 0}">status-opened</c:when>
                            <c:when test="${position.posStatus == 1}">status-filled</c:when>
                            <c:when test="${position.posStatus == 2}">status-closed</c:when>
                        </c:choose>">
                        <c:choose>
                            <c:when test="${position.posStatus == 0}">OPENED</c:when>
                            <c:when test="${position.posStatus == 1}">FILLED</c:when>
                            <c:when test="${position.posStatus == 2}">CLOSED</c:when>
                        </c:choose>
                    </span>
                </div>

                <div class="action-section">
                    <c:set var="canApply" value="${(position.appStatus == -1 || position.appId == null || (position.appStatus == 3 && position.appId != null))}"/>

                    <button class="btn-apply"
                            id="applyBtn"
                            ${!canApply ? 'disabled' : ''}
                            data-posid="${position.posId}">
                        Apply Now
                    </button>

                    <c:if test="${position.appId != null && position.appStatus == 0}">
                        <button class="btn-withdraw-detail"
                                id="withdrawBtn"
                                data-appid="${position.appId}">
                            Withdraw
                        </button>
                    </c:if>
                </div>

                <c:if test="${position.appStatus != -1 && position.appId != null}">
                    <div class="application-info">
                        <h3 class="section-title">Application Information</h3>

                        <div class="app-detail-grid">
                            <div class="grid-item">
                                <span class="grid-label">Application Status</span>
                                <span class="status-badge
                                    <c:choose>
                                        <c:when test="${position.appStatus == 0}">status-applied</c:when>
                                        <c:when test="${position.appStatus == 1}">status-offered</c:when>
                                        <c:when test="${position.appStatus == 2}">status-rejected</c:when>
                                        <c:when test="${position.appStatus == 3}">status-withdrawn</c:when>
                                    </c:choose>"
                                    id="appStatusBadge">
                                    <c:choose>
                                        <c:when test="${position.appStatus == 0}">APPLIED</c:when>
                                        <c:when test="${position.appStatus == 1}">OFFERED</c:when>
                                        <c:when test="${position.appStatus == 2}">REJECTED</c:when>
                                        <c:when test="${position.appStatus == 3}">WITHDRAWN</c:when>
                                    </c:choose>
                                </span>
                            </div>
                            <div class="grid-item">
                                <span class="grid-label">Applied At</span>
                                <span class="grid-value"><fmt:formatDate value="${position.applyAt}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
                            </div>
                        </div>

                        <c:if test="${not empty position.feedback}">
                            <div class="feedback-section">
                                <span class="grid-label">Feedback</span>
                                <p class="feedback-content">${position.feedback}</p>
                            </div>
                        </c:if>
                    </div>
                </c:if>
            </div>
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

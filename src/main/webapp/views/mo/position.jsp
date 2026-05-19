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
    <title>Position</title>
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
                <a href="moServlet?action=listPosition&page=1&filter=all&order=postDate" class="nav-item">Home</a>
                <a href="moServlet?action=post" class="nav-item">Post Position</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <!-- Back Button -->
            <button onclick="goBack()" class="btn-back">&larr; Back to List</button>

            <!-- Tab Navigation -->
            <div class="tab-navigation">
                <button class="tab-btn active" data-tab="approval" onclick="switchTab('approval')">
                    Approval
                </button>
                <button class="tab-btn" data-tab="position" onclick="switchTab('position')">
                    Position
                </button>
            </div>

            <!-- Tab 1: Approval List (Default) -->
            <div id="approvalTab" class="tab-content active">
                <div class="approval-header">
                    <h2 class="approval-title">Applications</h2>
                    <div class="page-controls">
                        <div class="control-item">
                            <label for="appFilterSelect">Filter:</label>
                            <select id="appFilterSelect" class="control-select">
                                <option value="all" selected>All</option>
                                <option value="opened">Opened</option>
                                <option value="offered">Offered</option>
                                <option value="rejected">Rejected</option>
                            </select>
                        </div>
                        <div class="control-item">
                            <label for="appOrderSelect">Sort by:</label>
                            <select id="appOrderSelect" class="control-select">
                                <option value="applyAt" selected>Apply Time</option>
                                <option value="recommend">Recommendation</option>
                            </select>
                        </div>
                    </div>
                </div>
                <div id="approvalListContainer">
                    <p class="loading-text">Loading applications...</p>
                </div>
            </div>

            <!-- Tab 2: Position Detail -->
            <div id="positionTab" class="tab-content">
                <div class="position-detail">
                    <h2 class="detail-title">${position.title}</h2>
                    <p class="detail-module">${position.moduleCode} - ${position.moduleName}</p>

                    <div class="detail-section">
                        <h3 class="section-title">Description</h3>
                        <p class="section-content">${position.description}</p>
                    </div>

                    <div class="detail-grid">
                        <div class="grid-item">
                            <span class="grid-label">Weekly Workload</span>
                            <span class="grid-value">${position.weeklyWorkload} hours</span>
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
                        <div class="grid-item">
                            <span class="grid-label">Applied</span>
                            <span class="grid-value">${position.appliedNum}</span>
                        </div>
                        <div class="grid-item">
                            <span class="grid-label">Rejected</span>
                            <span class="grid-value">${position.rejectedNum}</span>
                        </div>
                    </div>

                    <div class="detail-section">
                        <h3 class="section-title">Grade Requirements</h3>
                        <div class="detail-grid">
                            <div class="grid-item">
                                <span class="grid-label">Minimum Degree</span>
                                <span class="grid-value">
                                    <c:choose>
                                        <c:when test="${position.minGrade == -1}">Unlimited</c:when>
                                        <c:otherwise>
                                            <c:choose>
                                                <c:when test="${position.minGrade <= 4}">Bachelor (Year ${position.minGrade})</c:when>
                                                <c:when test="${position.minGrade <= 7}">Master (Year ${position.minGrade - 5})</c:when>
                                                <c:otherwise>PhD (Year ${position.minGrade - 8})</c:otherwise>
                                            </c:choose>
                                        </c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                            <div class="grid-item">
                                <span class="grid-label">Maximum Degree</span>
                                <span class="grid-value">
                                    <c:choose>
                                        <c:when test="${position.maxGrade == 2147483647}">Unlimited</c:when>
                                        <c:otherwise>
                                            <c:choose>
                                                <c:when test="${position.maxGrade <= 4}">Bachelor (Year ${position.maxGrade})</c:when>
                                                <c:when test="${position.maxGrade <= 7}">Master (Year ${position.maxGrade - 5})</c:when>
                                                <c:otherwise>PhD (Year ${position.maxGrade - 8})</c:otherwise>
                                            </c:choose>
                                        </c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </div>
                    </div>

                    <div class="detail-dates">
                        <div class="date-row">
                            <span class="date-label">Start Date:</span>
                            <span class="date-value"><fmt:formatDate value="${position.startDate}"
                                                                     pattern="yyyy-MM-dd"/></span>
                        </div>
                        <div class="date-row">
                            <span class="date-label">End Date:</span>
                            <span class="date-value"><fmt:formatDate value="${position.endDate}"
                                                                     pattern="yyyy-MM-dd"/></span>
                        </div>
                        <div class="date-row">
                            <span class="date-label">Deadline:</span>
                            <span class="date-value deadline"><fmt:formatDate value="${position.deadline}"
                                                                              pattern="yyyy-MM-dd"/></span>
                        </div>
                    </div>

                    <div class="detail-section">
                        <h3 class="section-title">Required Skills</h3>
                        <div class="skills-list">
                            <c:forEach var="skill" items="${position.skills}">
                                <span class="skill-tag">${skill}</span>
                            </c:forEach>
                        </div>
                    </div>

                    <div class="detail-status">
                        <span class="status-badge
                            <c:choose>
                                <c:when test="${position.status == 0}">status-opened</c:when>
                                <c:when test="${position.status == 1}">status-filled</c:when>
                                <c:when test="${position.status == 2}">status-closed</c:when>
                                <c:when test="${position.status == 3}">status-withdrawn</c:when>
                            </c:choose>">
                            <c:choose>
                                <c:when test="${position.status == 0}">OPENED</c:when>
                                <c:when test="${position.status == 1}">FILLED</c:when>
                                <c:when test="${position.status == 2}">CLOSED</c:when>
                                <c:when test="${position.status == 3}">WITHDRAWN</c:when>
                            </c:choose>
                        </span>
                    </div>
                    <div>
                        <c:if test="${position.status == 0}">
                            <button onclick="withdrawPosition('${posId}')" class="btn btn-withdraw-position">
                                Withdraw Position
                            </button>
                        </c:if>
                        <c:if test="${position.status == 3}">
                            <a href="moServlet?action=repostPosition&posId=${posId}" class="btn btn-repost-position">
                                Repost Position
                            </a>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- Profile Modal -->
<div id="profileModal" class="modal" style="display:none;">
    <div class="modal-content modal-large">
        <span class="close-modal">&times;</span>
        <div id="profileContent"></div>
        <div class="feedback-section">
            <label for="feedbackInput">Feedback:</label>
            <textarea id="feedbackInput" placeholder="Enter feedback (optional)..."></textarea>
        </div>
        <div class="modal-actions">
            <button onclick="handleOffer()" class="btn btn-offer">Offer</button>
            <button onclick="handleReject()" class="btn btn-reject">Reject</button>
        </div>
    </div>
</div>

<!-- Feedback Confirmation Modal -->
<div id="feedbackModal" class="modal" style="display:none;">
    <div class="modal-content">
        <span class="close-feedback-modal">&times;</span>
        <h3 id="feedbackModalTitle">Confirm Action</h3>
        <p>Please provide feedback (optional):</p>
        <textarea id="quickFeedbackInput" placeholder="Enter feedback..."></textarea>
        <div class="modal-actions">
            <button onclick="confirmAction()" class="btn btn-confirm">Confirm</button>
            <button onclick="closeFeedbackModal()" class="btn btn-cancel">Cancel</button>
        </div>
    </div>
</div>

<!-- Message Modal -->
<div id="messageModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-message-modal">&times;</span>
        <h3 id="modalTitle">Notice</h3>
        <p id="modalBody">Successfully Post Position.</p>
        <button class="btn btn-confirm" id="confirmMessageModal">OK</button>
    </div>
</div>

<script>
    const currentPosId = '${posId}';
    const fromCondition = {
        page: ${fromCondition.page != null ? fromCondition.page : 1},
        filter: '${fromCondition.filter != null ? fromCondition.filter : "all"}',
        order: '${fromCondition.order != null ? fromCondition.order : "postDate"}'
    };
    let currentAppPage = 1;
    let currentProfileData = null;
    let pendingAction = null;
    let pendingAppId = null;
    let currentFilter = 'all';
    let currentOrder = 'applyAt';

    // Load approval list on page load
    $(document).ready(function () {
        // Bind dropdown change events for position page
        $('#appFilterSelect').change(function () {
            setPositionFilter($(this).val());
        });

        $('#appOrderSelect').change(function () {
            setPositionOrder($(this).val());
        });

        loadApprovalList(1);
    });
</script>

<script src="static/js/mo.js"></script>
</body>
</html>

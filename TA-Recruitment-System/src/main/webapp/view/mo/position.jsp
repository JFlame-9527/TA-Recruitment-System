<%--
  @author: 477996850
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
                <a href="moServlet?action=listPosition&page=1" class="nav-item">Home</a>
                <a href="moServlet?action=postPosition" class="nav-item">Post Position</a>
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

                    <div class="detail-dates">
                        <div class="date-row">
                            <span class="date-label">Start Date:</span>
                            <span class="date-value"><fmt:formatDate value="${position.startDate}" pattern="yyyy-MM-dd"/></span>
                        </div>
                        <div class="date-row">
                            <span class="date-label">End Date:</span>
                            <span class="date-value"><fmt:formatDate value="${position.endDate}" pattern="yyyy-MM-dd"/></span>
                        </div>
                        <div class="date-row">
                            <span class="date-label">Deadline:</span>
                            <span class="date-value deadline"><fmt:formatDate value="${position.deadline}" pattern="yyyy-MM-dd"/></span>
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
    const fromPage = '${fromPage}';
    let currentAppPage = 1;
    let currentProfileData = null;
    let pendingAction = null; // 'offer' or 'reject'
    let pendingAppId = null;

    // Load approval list on page load
    $(document).ready(function() {
        loadApprovalList(1);
    });
</script>

<script src="static/js/mo.js"></script>
</body>
</html>

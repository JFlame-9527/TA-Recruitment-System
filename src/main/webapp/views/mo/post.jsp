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
    <title>${isRepost ? 'Repost Position' : 'Post Position'}</title>
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
                <a href="moServlet?action=post" class="nav-item active">Post Position</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">${isRepost ? 'Repost Position' : 'Post New Position'}</h1>

            <c:if test="${isRepost}">
                <div class="alert alert-info">
                    <strong>📋 Reposting Position:</strong> ${repostData.title}
                    <p class="form-hint">All fields are pre-filled from the withdrawn position. Modify as needed and submit to create a new position.</p>
                </div>
            </c:if>

            <form id="postPositionForm" class="position-form">
                <input type="hidden" name="action" value="postPosition">

                <div class="form-section">
                    <h3 class="section-title">Basic Information</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="title">Position Title <span class="required">*</span></label>
                            <input type="text" id="title" name="title" class="form-control"
                                   placeholder="e.g., Teaching Assistant for EBU6304"
                                   value="${not empty repostData ? repostData.title : ''}" required>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="moduleCode">Module Code <span class="required">*</span></label>
                            <input type="text" id="moduleCode" name="moduleCode" class="form-control"
                                   placeholder="e.g., EBU6304"
                                   value="${not empty repostData ? repostData.moduleCode : ''}" required>
                        </div>

                        <div class="form-group">
                            <label for="moduleName">Module Name <span class="required">*</span></label>
                            <input type="text" id="moduleName" name="moduleName" class="form-control"
                                   placeholder="e.g., Software Engineering"
                                   value="${not empty repostData ? repostData.moduleName : ''}" required>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Position Details</h3>

                    <div class="form-group">
                        <label for="description">Description <span class="required">*</span></label>
                        <textarea id="description" name="description" class="form-control textarea-large"
                                  placeholder="Describe the position responsibilities and requirements..."
                                  rows="6" required>${not empty repostData ? repostData.description : ''}</textarea>
                    </div>

                    <div class="form-group">
                        <label>Required Skills <span class="required">*</span></label>
                        <div id="skillsContainer" class="skills-input-container">
                            <div class="skill-input-wrapper">
                                <input type="text" id="skillInput" placeholder="Type skill and press Enter">
                                <button type="button" id="addSkillBtn">Add</button>
                            </div>
                        </div>
                        <div id="skillsList" class="skills-display-list"></div>
                        <div id="skillsHiddenFields"></div>
                        <small class="form-hint">Press Enter or click Add to add required skills</small>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Workload & Duration</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="weeklyWorkload">Weekly Workload (hours) <span class="required">*</span></label>
                            <input type="number" id="weeklyWorkload" name="weeklyWorkload" class="form-control"
                                   step="0.5" min="1" max="40" placeholder="e.g., 10"
                                   value="${not empty repostData ? repostData.weeklyWorkload : ''}" required>
                        </div>

                        <div class="form-group">
                            <label for="duration">Duration (weeks)</label>
                            <input type="text" id="duration" name="duration" class="form-control"
                                   readonly placeholder="Auto-calculated from dates">
                            <small class="form-hint">Calculated automatically based on start and end dates</small>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="requiredNum">Number of Positions <span class="required">*</span></label>
                            <input type="number" id="requiredNum" name="requiredNum" class="form-control"
                                   min="1" max="50" placeholder="e.g., 3"
                                   value="${not empty repostData ? repostData.requiredNum : ''}" required>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Grade Requirements</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label>Minimum Grade Requirement</label>
                            <select id="minDegree" name="minDegree" class="form-control">
                                <option value="unlimited" ${empty repostData || repostData.minGrade == -1 ? 'selected' : ''}>Unlimited</option>
                                <option value="BACHELOR" ${not empty repostData && repostData.minGrade >= 0 && repostData.minGrade <= 4 ? 'selected' : ''}>Bachelor</option>
                                <option value="MASTER" ${not empty repostData && repostData.minGrade >= 5 && repostData.minGrade <= 7 ? 'selected' : ''}>Master</option>
                                <option value="PHD" ${not empty repostData && repostData.minGrade >= 8 ? 'selected' : ''}>PhD</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="minYear">Minimum Year</label>
                            <input type="number" id="minYear" name="minYear" class="form-control"
                                   min="1" max="10" placeholder="e.g., 3"
                                   value="${not empty repostData && repostData.minGrade != -1 ? (repostData.minGrade <= 4 ? repostData.minGrade : (repostData.minGrade <= 7 ? repostData.minGrade - 5 : repostData.minGrade - 8)) : ''}"
                                   ${empty repostData || repostData.minGrade == -1 ? 'disabled' : ''}>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label>Maximum Grade Requirement</label>
                            <select id="maxDegree" name="maxDegree" class="form-control">
                                <option value="unlimited" ${empty repostData || repostData.maxGrade == 2147483647 ? 'selected' : ''}>Unlimited</option>
                                <option value="BACHELOR" ${not empty repostData && repostData.maxGrade >= 0 && repostData.maxGrade <= 4 ? 'selected' : ''}>Bachelor</option>
                                <option value="MASTER" ${not empty repostData && repostData.maxGrade >= 5 && repostData.maxGrade <= 7 ? 'selected' : ''}>Master</option>
                                <option value="PHD" ${not empty repostData && repostData.maxGrade >= 8 ? 'selected' : ''}>PhD</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="maxYear">Maximum Year</label>
                            <input type="number" id="maxYear" name="maxYear" class="form-control"
                                   min="1" max="10" placeholder="e.g., 5"
                                   value="${not empty repostData && repostData.maxGrade != 2147483647 ? (repostData.maxGrade <= 4 ? repostData.maxGrade : (repostData.maxGrade <= 7 ? repostData.maxGrade - 5 : repostData.maxGrade - 8)) : ''}"
                                   ${empty repostData || repostData.maxGrade == 2147483647 ? 'disabled' : ''}>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Timeline</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="startDate">Start Date <span class="required">*</span></label>
                            <input type="date" id="startDate" name="startDate" class="form-control"
                                   <c:if test="${not empty repostData}">
                                   value="<fmt:formatDate value='${repostData.startDate}' pattern='yyyy-MM-dd'/>"
                                   </c:if>
                                   required>
                        </div>

                        <div class="form-group">
                            <label for="endDate">End Date <span class="required">*</span></label>
                            <input type="date" id="endDate" name="endDate" class="form-control"
                                   <c:if test="${not empty repostData}">
                                   value="<fmt:formatDate value='${repostData.endDate}' pattern='yyyy-MM-dd'/>"
                                   </c:if>
                                   required>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="deadline">Application Deadline <span class="required">*</span></label>
                            <input type="date" id="deadline" name="deadline" class="form-control"
                                   <c:if test="${not empty repostData}">
                                   value="<fmt:formatDate value='${repostData.deadline}' pattern='yyyy-MM-dd'/>"
                                   </c:if>
                                   required>
                        </div>
                    </div>
                </div>

                <div id="formError" class="alert alert-danger" style="display: none;"></div>
                <div id="formSuccess" class="alert alert-success" style="display: none;"></div>

                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="cancelBtn">Cancel</button>
                    <button type="submit" class="btn btn-primary" id="submitBtn">${isRepost ? 'Repost Position' : 'Post Position'}</button>
                </div>
            </form>
        </div>
    </div>
</main>

<div id="messageModal" class="modal" style="display: none;">
    <div class="modal-content">
        <span class="close-message-modal">&times;</span>
        <h3 id="messageModalTitle">Notice</h3>
        <p id="messageModalBody">Successfully Post Position.</p>
        <button class="btn btn-confirm" id="confirmMessageModal">OK</button>
    </div>
</div>

<script>
    window.pageLoading = false;

    // Initialize repost data if exists
    <c:if test="${isRepost && not empty repostData}">
        $(document).ready(function() {
            // Scroll to top immediately and remove focus
            window.scrollTo(0, 0);
            if (document.activeElement) {
                document.activeElement.blur();
            }

            // Pre-fill skills from repost data
            <c:if test="${not empty repostData.skills}">
                // Initialize skills array first
                window.skills = [];

                var repostSkills = [
                    <c:forEach var="skill" items="${repostData.skills}" varStatus="status">
                        "<c:out value='${skill}'/>"<c:if test="${!status.last}">,</c:if>
                    </c:forEach>
                ];

                // Wait for initPostPositionForm to complete, then add skills
                setTimeout(function() {
                    repostSkills.forEach(function(skill) {
                        if (typeof window.addSkill === 'function') {
                            window.addSkill(skill);
                        }
                    });

                    // After skills are loaded, calculate duration
                    setTimeout(function() {
                        if ($('#startDate').val() && $('#endDate').val() && typeof window.calculateDuration === 'function') {
                            window.calculateDuration();
                        }
                        // Final scroll to top and blur
                        window.scrollTo(0, 0);
                        if (document.activeElement) {
                            document.activeElement.blur();
                        }
                    }, 50);
                }, 150);
            </c:if>

            // If no skills, still calculate duration
            <c:if test="${empty repostData.skills}">
                setTimeout(function() {
                    if ($('#startDate').val() && $('#endDate').val() && typeof window.calculateDuration === 'function') {
                        window.calculateDuration();
                    }
                    window.scrollTo(0, 0);
                    if (document.activeElement) {
                        document.activeElement.blur();
                    }
                }, 100);
            </c:if>
        });
    </c:if>
</script>

<script src="static/js/mo.js"></script>
</body>
</html>

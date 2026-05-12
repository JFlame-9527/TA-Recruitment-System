<%--
  @author: QiheSun
  @Since: 2026/5/12
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>TA Profile</title>
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
                <a href="taServlet?action=listPositions&page=1&filter=all&order=postDate" class="nav-item">Position</a>
                <a href="taServlet?action=getProfile" class="nav-item active">Profile</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">My Profile</h1>

            <c:choose>
                <c:when test="${empty profile}">
                    <div class="empty-state">
                        <div class="empty-icon">👤</div>
                        <div class="empty-text">No Profile Found</div>
                        <p style="color: #666; margin-bottom: 20px;">Create your profile to start applying for positions!</p>
                        <button class="btn btn-primary" id="createProfileBtn">Create Profile</button>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="profile-display">
                        <div class="profile-header">
                            <div class="profile-avatar">
                                <span>${profile.name.substring(0, 1)}</span>
                            </div>
                            <div class="profile-name-section">
                                <h2 class="profile-name">${profile.name}</h2>
                                <p class="profile-meta">${profile.college} - ${profile.major}</p>
                            </div>
                        </div>

                        <div class="profile-grid">
                            <div class="profile-item">
                                <label>Gender</label>
                                <span>${profile.gender}</span>
                            </div>
                            <div class="profile-item">
                                <label>Age</label>
                                <span>${profile.age}</span>
                            </div>
                            <div class="profile-item">
                                <label>Degree</label>
                                <span>${profile.degree}</span>
                            </div>
                            <div class="profile-item">
                                <label>Year</label>
                                <span>${profile.year}</span>
                            </div>
                            <div class="profile-item">
                                <label>Email</label>
                                <span>${profile.email}</span>
                            </div>
                            <div class="profile-item">
                                <label>Phone</label>
                                <span>${profile.phone}</span>
                            </div>
                        </div>

                        <div class="profile-section">
                            <h3 class="section-title">Skills</h3>
                            <div class="skills-list">
                                <c:forEach var="skill" items="${profile.skills}">
                                    <span class="skill-tag">${skill}</span>
                                </c:forEach>
                            </div>
                        </div>

                        <div class="profile-section" id="resumeSection">
                            <h3 class="section-title">Resume</h3>
                            <div class="resume-info">
                                <div class="resume-file">
                                    <span class="file-icon">📄</span>
                                    <span class="file-name">${profile.resumeName}</span>
                                </div>
                                <div class="resume-actions">
                                    <a href="${resumeUrl}" target="_blank" class="btn btn-secondary">View</a>
                                    <a href="taServlet?action=downloadResume&file=${profile.resumePath}&download=true"
                                       class="btn btn-secondary">Download</a>
                                </div>
                            </div>
                        </div>

                        <div class="profile-actions">
                            <button class="btn btn-primary"
                                    id="editProfileBtn"
                                    data-name="${profile.name}"
                                    data-gender="${profile.gender}"
                                    data-age="${profile.age}"
                                    data-college="${profile.college}"
                                    data-major="${profile.major}"
                                    data-degree="${profile.degree}"
                                    data-year="${profile.year}"
                                    data-email="${profile.email}"
                                    data-phone="${profile.phone}"
                                    data-resume-name="${profile.resumeName}">Edit Profile</button>
                        </div>

                        <div id="profileSkillsData" style="display: none;">
                            <c:choose>
                                <c:when test="${not empty profile.skills}">
                                    <c:forEach var="skill" items="${profile.skills}" varStatus="status">
                                        <span class="skill-data">${skill}</span>
                                    </c:forEach>
                                </c:when>
                            </c:choose>
                        </div>
                    </div>

                    <script type="application/json" id="profileSkillsData">
                        <c:choose>
                            <c:when test="${not empty profile.skills}">
                                [
                                    <c:forEach var="skill" items="${profile.skills}" varStatus="status">
                                        "${skill}"<c:if test="${!status.last}">,</c:if>
                                    </c:forEach>
                                ]
                            </c:when>
                            <c:otherwise>[]</c:otherwise>
                        </c:choose>
                    </script>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<div id="profileModal" class="modal" style="display: none;">
    <div class="modal-content modal-large">
        <span class="close-modal">&times;</span>
        <h3 id="modalTitle">Create Profile</h3>

        <form id="profileForm" enctype="multipart/form-data">
            <input type="hidden" name="action" value="createProfile">

            <div class="form-row">
                <div class="form-group">
                    <label for="name">Name <span class="required">*</span></label>
                    <input type="text" id="name" name="name" class="form-control" required>
                </div>

                <div class="form-group">
                    <label for="gender">Gender</label>
                    <select id="gender" name="gender" class="form-control">
                        <option value="">Select Gender</option>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="age">Age</label>
                    <input type="number" id="age" name="age" class="form-control" min="16" max="100">
                </div>

                <div class="form-group">
                    <label for="degree">Degree</label>
                    <select id="degree" name="degree" class="form-control">
                        <option value="">Select Degree</option>
                        <option value="BACHELOR">Bachelor</option>
                        <option value="MASTER">Master</option>
                        <option value="PHD">PhD</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="college">College</label>
                    <input type="text" id="college" name="college" class="form-control" placeholder="e.g., School of Engineering">
                </div>

                <div class="form-group">
                    <label for="major">Major</label>
                    <input type="text" id="major" name="major" class="form-control" placeholder="e.g., Computer Science">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="year">Year of Study</label>
                    <input type="number" id="year" name="year" class="form-control" min="1" max="10" placeholder="e.g., 3">
                </div>

                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="your.email@example.com">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="phone">Phone</label>
                    <input type="tel" id="phone" name="phone" class="form-control" placeholder="+44 123 456 7890">
                </div>

                <div class="form-group" style="visibility: hidden;">
                    <label>&nbsp;</label>
                    <div></div>
                </div>
            </div>

            <div class="form-group">
                <label>Skills</label>
                <div id="skillsContainer" class="skills-input-container">
                    <div class="skill-input-wrapper">
                        <input type="text" id="skillInput" placeholder="Type skill and press Enter">
                        <button type="button" id="addSkillBtn">Add</button>
                    </div>
                </div>
                <div id="skillsHiddenFields"></div>
                <small class="form-hint">Press Enter or click Add to add skills</small>
            </div>

            <div class="form-group">
                <label for="resume">Resume (PDF, Max 10MB) <span class="required" id="resumeRequired">*</span></label>
                <input type="file" id="resume" name="resume" accept=".pdf,application/pdf" class="form-control">
                <button type="button" id="extractSkillsBtn" class="btn btn-secondary" style="margin-top: 10px; display: none;">
                    Extract Skills
                </button>
                <div id="currentResume" style="display: none; margin-top: 10px; padding: 10px; background: #f5f5f5; border-radius: 6px;">
                    <span>Current: </span>
                    <span id="currentResumeName"></span>
                    <small class="form-hint">Upload a new file to replace</small>
                </div>
                <small class="form-hint" id="resumeHint">Only PDF files are accepted</small>
            </div>

            <div id="extractStatus" class="alert alert-info" style="display: none;"></div>

            <div id="formError" class="alert alert-danger" style="display: none;"></div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" id="cancelBtn">Cancel</button>
                <button type="submit" class="btn btn-primary" id="submitBtn">Save</button>
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

<script>
    window.pageLoading = false;
    window.isEditMode = false;
</script>

<script src="static/js/ta.js"></script>
</body>
</html>


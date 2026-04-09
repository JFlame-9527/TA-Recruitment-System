<%--
  @author: 477996850
  @Since: 2026/4/5
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="<%=request.getContextPath() + "/"%>">
    <title>Post Position</title>
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
                <a href="moServlet?action=postPosition" class="nav-item active">Post Position</a>
            </nav>
        </div>
    </div>
</header>

<main class="main-content">
    <div class="content-wrapper">
        <div class="main-container">
            <h1 class="page-title">Post New Position</h1>

            <form id="postPositionForm" class="position-form">
                <input type="hidden" name="action" value="createPosition">

                <div class="form-section">
                    <h3 class="section-title">Basic Information</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="title">Position Title <span class="required">*</span></label>
                            <input type="text" id="title" name="title" class="form-control"
                                   placeholder="e.g., Teaching Assistant for EBU6304" required>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="moduleCode">Module Code <span class="required">*</span></label>
                            <input type="text" id="moduleCode" name="moduleCode" class="form-control"
                                   placeholder="e.g., EBU6304" required>
                        </div>

                        <div class="form-group">
                            <label for="moduleName">Module Name <span class="required">*</span></label>
                            <input type="text" id="moduleName" name="moduleName" class="form-control"
                                   placeholder="e.g., Software Engineering" required>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Position Details</h3>

                    <div class="form-group">
                        <label for="description">Description <span class="required">*</span></label>
                        <textarea id="description" name="description" class="form-control textarea-large"
                                  placeholder="Describe the position responsibilities and requirements..."
                                  rows="6" required></textarea>
                    </div>

                    <div class="form-group">
                        <label>Required Skills</label>
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
                                   step="0.5" min="1" max="40" placeholder="e.g., 10" required>
                        </div>

                        <div class="form-group">
                            <label for="duration">Duration (weeks) <span class="required">*</span></label>
                            <input type="number" id="duration" name="duration" class="form-control"
                                   min="1" max="52" placeholder="e.g., 12" required>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="requiredNum">Number of Positions <span class="required">*</span></label>
                            <input type="number" id="requiredNum" name="requiredNum" class="form-control"
                                   min="1" max="50" placeholder="e.g., 3" required>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h3 class="section-title">Timeline</h3>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="startDate">Start Date <span class="required">*</span></label>
                            <input type="date" id="startDate" name="startDate" class="form-control" required>
                        </div>

                        <div class="form-group">
                            <label for="endDate">End Date <span class="required">*</span></label>
                            <input type="date" id="endDate" name="endDate" class="form-control" required>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="deadline">Application Deadline <span class="required">*</span></label>
                            <input type="date" id="deadline" name="deadline" class="form-control" required>
                        </div>
                    </div>
                </div>

                <div id="formError" class="alert alert-danger" style="display: none;"></div>
                <div id="formSuccess" class="alert alert-success" style="display: none;"></div>

                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="cancelBtn">Cancel</button>
                    <button type="submit" class="btn btn-primary" id="submitBtn">Post Position</button>
                </div>
            </form>
        </div>
    </div>
</main>

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
    var skills = [];
</script>

<script src="static/js/mo.js"></script>
</body>
</html>

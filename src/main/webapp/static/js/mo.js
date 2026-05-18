$(document).ready(function() {
    // User dropdown toggle
    const $userInfo = $('.user-info');
    const $userDropdown = $('.user-dropdown');

    $userInfo.click(function(e) {
        e.stopPropagation();
        $userDropdown.toggleClass('show');
    });

    $(document).click(function() {
        $userDropdown.removeClass('show');
    });

    $userDropdown.click(function(e) {
        e.stopPropagation();
    });

    // Exit functionality
    $('.dropdown-item.exit').click(function() {
        if (confirm('Are you sure you want to exit?')) {
            window.location.href = 'userServlet?action=logout';
        }
    });

    // Edit profile functionality
    $('.dropdown-item.edit').click(function() {
        alert('Edit profile - to be implemented');
    });

    // Pagination click handler
    $(document).on('click', '.page-item:not(.active)', function(e) {
        e.preventDefault();
        const page = $(this).data('page');
        if (page) {
            const filter = typeof currentFilter !== 'undefined' ? currentFilter : 'all';
            const order = typeof currentOrder !== 'undefined' ? currentOrder : 'postDate';
            window.location.href = 'moServlet?action=listPosition&page=' + page + '&filter=' + filter + '&order=' + order;
        }
    });

    // Withdraw position from home page
    $(document).on('click', '.btn-withdraw', function() {
        const posId = $(this).data('posid');
        const page = $(this).data('page');
        const $card = $(this).closest('.position-card');

        if (confirm('Are you sure you want to withdraw this position? This action cannot be undone and will delete all applications.')) {
            $.ajax({
                url: 'moServlet',
                type: 'POST',
                data: {
                    action: 'withdrawnPosition',
                    posId: posId
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        // Update status to withdrawn (3)
                        $card.find('.status-badge')
                            .removeClass('status-opened status-filled status-closed')
                            .addClass('status-withdrawn')
                            .text('WITHDRAWN');

                        // Remove withdraw button
                        $card.find('.btn-withdraw').remove();

                        showMessage('Success', response.message || 'Position withdrawn successfully');
                    } else {
                        showMessage('Error', response.message || 'Failed to withdraw position');
                    }
                },
                error: function(xhr) {
                    const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Failed to withdraw position';
                    showMessage('Error', errorMsg);
                }
            });
        }
    });

    // Bind modal close buttons using event delegation
    // Profile modal close button (×)
    $(document).on('click', '#profileModal .close-modal', function() {
        closeProfileModal();
    });
    
    // Feedback modal close button (×)
    $(document).on('click', '.close-feedback-modal', function() {
        closeFeedbackModal();
    });
    
    // Message modal close button (×) - backup for showMessage function
    $(document).on('click', '#messageModal .close-message-modal', function() {
        $('#messageModal').fadeOut(200);
    });

    // Initialize post position form if on post page
    initPostPositionForm();
});

// View position details
function viewPosition(posId, page) {
    const filter = typeof currentFilter !== 'undefined' ? currentFilter : 'all';
    const order = typeof currentOrder !== 'undefined' ? currentOrder : 'postDate';
    window.location.href = 'moServlet?action=positionDetail&posId=' + posId + '&page=' + page + '&filter=' + filter + '&order=' + order;
}

// Set filter for home page
function setHomeFilter(filter) {
    currentFilter = filter;
    window.location.href = 'moServlet?action=listPosition&page=1&filter=' + filter + '&order=' + currentOrder;
}

// Set order for home page
function setHomeOrder(order) {
    currentOrder = order;
    window.location.href = 'moServlet?action=listPosition&page=1&filter=' + currentFilter + '&order=' + order;
}

// Initialize post position form
function initPostPositionForm() {
    if ($('#postPositionForm').length === 0) {
        return;
    }

    // Skill management
    const $skillInput = $('#skillInput');
    const $addSkillBtn = $('#addSkillBtn');
    const $skillsList = $('#skillsList');
    const $skillsHiddenFields = $('#skillsHiddenFields');
    
    // Initialize skills array
    window.skills = window.skills || [];

    function addSkill(skill) {
        skill = skill.trim();
        if (!skill) return;
        
        if (window.skills.includes(skill)) {
            showMessage('Warning', 'This skill already exists');
            return;
        }

        window.skills.push(skill);
        renderSkills();
        $skillInput.val('');
        $skillInput.focus();
    }

    function removeSkill(index) {
        window.skills.splice(index, 1);
        renderSkills();
    }

    function renderSkills() {
        $skillsList.empty();
        $skillsHiddenFields.empty();

        window.skills.forEach((skill, index) => {
            $skillsList.append(`
                <div class="skill-tag-item">
                    ${skill}
                    <span class="remove-skill" onclick="removeSkill(${index})">&times;</span>
                </div>
            `);
            
            $skillsHiddenFields.append(`
                <input type="hidden" name="skills" value="${skill}">
            `);
        });
    }

    $addSkillBtn.click(function() {
        addSkill($skillInput.val());
    });

    $skillInput.keypress(function(e) {
        if (e.which === 13) {
            e.preventDefault();
            addSkill($(this).val());
        }
    });

    // Make functions globally accessible
    window.addSkill = addSkill;
    window.removeSkill = removeSkill;

    // Grade requirement management
    const $minDegree = $('#minDegree');
    const $minYear = $('#minYear');
    const $maxDegree = $('#maxDegree');
    const $maxYear = $('#maxYear');

    function handleDegreeChange($degreeSelect, $yearInput) {
        if ($degreeSelect.val() === 'unlimited') {
            $yearInput.prop('disabled', true).val('');
        } else {
            $yearInput.prop('disabled', false);
        }
    }

    $minDegree.change(function() {
        handleDegreeChange($(this), $minYear);
    });

    $maxDegree.change(function() {
        handleDegreeChange($(this), $maxYear);
    });

    // Duration calculation
    window.calculateDuration = function() {
        const startDateStr = $('#startDate').val();
        const endDateStr = $('#endDate').val();

        if (!startDateStr || !endDateStr) {
            $('#duration').val('');
            return;
        }

        const startDate = new Date(startDateStr);
        const endDate = new Date(endDateStr);

        if (startDate >= endDate) {
            $('#duration').val('');
            return;
        }

        // Calculate weeks with Sunday as boundary
        // Find the Sunday before or on start date
        const startDay = startDate.getDay(); // 0 is Sunday
        const daysToPreviousSunday = startDay;
        const adjustedStartDate = new Date(startDate);
        adjustedStartDate.setDate(startDate.getDate() - daysToPreviousSunday);

        // Find the Sunday after or on end date
        const endDay = endDate.getDay();
        const daysToNextSunday = endDay === 0 ? 0 : 7 - endDay;
        const adjustedEndDate = new Date(endDate);
        adjustedEndDate.setDate(endDate.getDate() + daysToNextSunday);

        // Calculate weeks
        const diffTime = adjustedEndDate.getTime() - adjustedStartDate.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        const weeks = Math.ceil(diffDays / 7);

        $('#duration').val(weeks);
    };

    $('#startDate, #endDate').change(function() {
        window.calculateDuration();
        validateDates();
    });

    // Form submission
    $('#postPositionForm').submit(function(e) {
        e.preventDefault();
        
        const $form = $(this);
        const $submitBtn = $('#submitBtn');
        const $errorDiv = $('#formError');
        const $successDiv = $('#formSuccess');

        $errorDiv.hide();
        $successDiv.hide();

        if (window.skills.length === 0) {
            $errorDiv.text('Please add at least one required skill').show();
            return;
        }

        if (!validateDates()) {
            return;
        }

        // Calculate minGrade and maxGrade
        const minDegree = $minDegree.val();
        const maxDegree = $maxDegree.val();
        const minYear = parseInt($minYear.val()) || 0;
        const maxYear = parseInt($maxYear.val()) || 0;

        let calculatedMinGrade = -1;
        let calculatedMaxGrade = 2147483647; // Integer.MAX_VALUE

        if (minDegree !== 'unlimited') {
            const offset = getGradeOffset(minDegree);
            calculatedMinGrade = minYear + offset;
        }

        if (maxDegree !== 'unlimited') {
            const offset = getGradeOffset(maxDegree);
            calculatedMaxGrade = maxYear + offset;
        }

        if (calculatedMinGrade > calculatedMaxGrade) {
            $errorDiv.text('Minimum grade cannot be greater than maximum grade').show();
            return;
        }

        const formData = new FormData($form[0]);
        
        formData.delete('skills');
        window.skills.forEach(function(skill) {
            formData.append('skills', skill);
        });

        const startDate = $('#startDate').val().replace(/\//g, '-');
        const endDate = $('#endDate').val().replace(/\//g, '-');
        const deadline = $('#deadline').val().replace(/\//g, '-');
        
        formData.set('startDate', startDate + ' 00:00:00');
        formData.set('endDate', endDate + ' 00:00:00');
        formData.set('deadline', deadline + ' 00:00:00');

        // Add calculated values
        formData.set('minGrade', calculatedMinGrade.toString());
        formData.set('maxGrade', calculatedMaxGrade.toString());

        $submitBtn.prop('disabled', true).text('Posting...');

        $.ajax({
            url: 'moServlet',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    $('#messageModalTitle').text('Success');
                    $('#messageModalBody').text('Position has been posted successfully!');
                    $('#messageModal').fadeIn(200);
                    
                    var redirectHome = function() {
                        window.location.href = 'moServlet?action=listPosition&page=1&filter=all&order=postDate';
                    };
                    
                    $('#confirmMessageModal').off('click').on('click', function() {
                        $('#messageModal').fadeOut(200, redirectHome);
                    });
                    
                    $('.close-modal').off('click').on('click', function() {
                        $('#messageModal').fadeOut(200, redirectHome);
                    });
                    
                    $(window).off('click.closeModal').on('click.closeModal', function(e) {
                        if ($(e.target).is('#messageModal')) {
                            $('#messageModal').fadeOut(200, redirectHome);
                            $(window).off('click.closeModal');
                        }
                    });
                } else {
                    $errorDiv.text(response.message || 'Failed to post position').show();
                    $submitBtn.prop('disabled', false).text('Post Position');
                }
            },
            error: function(xhr) {
                const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error occurred';
                $errorDiv.text(errorMsg).show();
                $submitBtn.prop('disabled', false).text('Post Position');
            }
        });
    });

    // Helper function to get grade offset
    function getGradeOffset(degree) {
        switch (degree.toUpperCase()) {
            case 'BACHELOR': return 0;
            case 'MASTER': return 10;
            case 'PHD': return 20;
            default: return 0;
        }
    }

    // Cancel button
    $('#cancelBtn').click(function() {
        if (confirm('Are you sure you want to cancel? All entered data will be lost.')) {
            window.location.href = 'moServlet?action=listPosition&page=1&filter=all&order=postDate';
        }
    });

    // Date validation
    $('#startDate, #endDate, #deadline').change(function() {
        validateDates();
    });

    function validateDates() {
        const startDate = new Date($('#startDate').val());
        const endDate = new Date($('#endDate').val());
        const deadline = new Date($('#deadline').val());

        if (startDate && endDate && startDate >= endDate) {
            $('#formError').text('End date must be after start date').show();
            return false;
        }

        if (startDate && deadline && deadline >= startDate) {
            $('#formError').text('Application deadline must be before start date').show();
            return false;
        }

        if (endDate && deadline && deadline >= endDate) {
            $('#formError').text('Application deadline must be before end date').show();
            return false;
        }

        $('#formError').hide();
        return true;
    }
}

// Show message with optional callback
function showMessage(title, message, callback) {
    $('#modalTitle').text(title);
    $('#modalBody').text(message);
    $('#messageModal').fadeIn(200);
    
    // Bind confirm button
    $('#confirmMessageModal').off('click').on('click', function() {
        $('#messageModal').fadeOut(200);
        if (callback && typeof callback === 'function') {
            callback();
        }
    });
    
    // Bind close button
    $('.close-message-modal').off('click').on('click', function() {
        $('#messageModal').fadeOut(200);
        if (callback && typeof callback === 'function') {
            callback();
        }
    });
}

// Close modal when clicking outside
$(window).on('click', function(e) {
    if ($(e.target).hasClass('modal')) {
        $(e.target).fadeOut(200);
    }
});

// Go back to position list
function goBack() {
    const page = fromCondition.page || 1;
    const filter = fromCondition.filter || 'all';
    const order = fromCondition.order || 'postDate';
    window.location.href = 'moServlet?action=listPosition&page=' + page + '&filter=' + filter + '&order=' + order;
}

// Tab switching
function switchTab(tabName) {
    $('.tab-btn').removeClass('active');
    $(`.tab-btn[data-tab="${tabName}"]`).addClass('active');

    $('.tab-content').removeClass('active');
    $(`#${tabName}Tab`).addClass('active');

    if (tabName === 'approval') {
        if ($('#approvalListContainer').children().length === 0 ||
            $('#approvalListContainer').find('.loading-text').length > 0) {
            loadApprovalList(1);
        }
    }
}

// Load application list
function loadApprovalList(page) {
    currentAppPage = page;

    $('#approvalListContainer').html('<p class="loading-text">Loading applications...</p>');

    $.ajax({
        url: 'moServlet',
        data: {
            action: 'listApp',
            posId: currentPosId,
            page: page,
            filter: currentFilter,
            order: currentOrder
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                // Extract condition from response and update dropdowns
                if (response.data.condition) {
                    currentFilter = response.data.condition.filter || 'all';
                    currentOrder = response.data.condition.order || 'applyAt';
                    currentAppPage = response.data.condition.page || 1;

                    // Update dropdown selections
                    if ($('#appFilterSelect').length > 0) {
                        $('#appFilterSelect').val(currentFilter);
                    }
                    if ($('#appOrderSelect').length > 0) {
                        $('#appOrderSelect').val(currentOrder);
                    }
                }
                renderApprovalList(response.data);
            } else {
                $('#approvalListContainer').html(
                    '<p class="error-text">Failed to load applications</p>'
                );
            }
        },
        error: function() {
            $('#approvalListContainer').html(
                '<p class="error-text">Network error</p>'
            );
        }
    });
}

// Set filter for position page (approval list)
function setPositionFilter(filter) {
    currentFilter = filter;
    loadApprovalList(1);
}

// Set order for position page (approval list)
function setPositionOrder(order) {
    currentOrder = order;
    loadApprovalList(1);
}

// Render application list
function renderApprovalList(data) {
    const appList = data.appList;
    const currentPage = data.currentPage;
    const totalPages = data.totalPages;

    if (!appList || appList.length === 0) {
        $('#approvalListContainer').html(
            '<div class="empty-state-detail">' +
            '<div class="empty-icon">📋</div>' +
            '<p>No applications found</p>' +
            '</div>'
        );
        return;
    }

    let html = '<div class="application-list">';
    appList.forEach(app => {
        html += `
            <div class="app-item" data-appid="${app.appId}" data-proid="${app.proId}">
                <div class="app-header">
                    <div class="app-info">
                        <h4>${app.name}</h4>
                        <p class="app-meta">${app.college}</p>
                        <p class="app-meta">${app.major}</p>
                        <p class="app-meta">Grade: ${app.grade}</p>
                        <p class="app-meta">Applied: ${formatDate(app.applyAt)}</p>
                    </div>
                    <div class="app-status">
                        <span class="status-badge status-${getStatusClass(app.status)}">
                            ${getStatusText(app.status)}
                        </span>
                    </div>
                </div>
                <div class="app-actions">
                    <button onclick="viewProfile('${app.proId}', '${app.appId}')" 
                            class="btn-view-profile">
                        👁 View Profile
                    </button>
                    ${app.status === 0 ? `
                        <button onclick="quickOffer('${app.appId}')" class="btn-offer-small">
                            ✓ Offer
                        </button>
                        <button onclick="quickReject('${app.appId}')" class="btn-reject-small">
                            ✗ Reject
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    });
    html += '</div>';

    if (totalPages > 1) {
        html += renderPagination(currentPage, totalPages);
    }

    $('#approvalListContainer').html(html);
}

// Render pagination
function renderPagination(currentPage, totalPages) {
    let html = '<div class="pagination"><ul>';

    if (currentPage > 1) {
        html += `<li class="page-item" onclick="loadApprovalList(${currentPage - 1})">
                    <a href="#">&laquo;</a>
                 </li>`;
    }

    for (let i = 1; i <= totalPages; i++) {
        const activeClass = i === currentPage ? 'active' : '';
        html += `<li class="page-item ${activeClass}" onclick="loadApprovalList(${i})">
                    <a href="#">${i}</a>
                 </li>`;
    }

    if (currentPage < totalPages) {
        html += `<li class="page-item" onclick="loadApprovalList(${currentPage + 1})">
                    <a href="#">&raquo;</a>
                 </li>`;
    }

    html += '</ul></div>';
    return html;
}

// View profile
function viewProfile(proId, appId) {
    $.ajax({
        url: 'moServlet',
        data: {
            action: 'getProfile',
            proId: proId,
            appId: appId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                currentProfileData = response.data;
                currentProfileData.appId = appId;
                showProfileModal(response.data);
            } else {
                showMessage('Error', response.message || 'Failed to load profile');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

// Show profile modal
function showProfileModal(profile) {
    const html = `
        <div class="profile-header">
            <h3>${profile.name}'s Profile</h3>
        </div>
        <div class="profile-body">
            <div class="profile-field">
                <strong>Email</strong>
                <p>${profile.email || 'N/A'}</p>
            </div>
            <div class="profile-field">
                <strong>Phone</strong>
                <p>${profile.phone || 'N/A'}</p>
            </div>
            <div class="profile-field">
                <strong>College</strong>
                <p>${profile.college || 'N/A'}</p>
            </div>
            <div class="profile-field">
                <strong>Major</strong>
                <p>${profile.major || 'N/A'}</p>
            </div>
            <div class="profile-field">
                <strong>Grade</strong>
                <p>${profile.grade || 'N/A'}</p>
            </div>
            <div class="profile-field">
                <strong>Skills</strong>
                <p>${profile.skills && profile.skills.length > 0 ? 
                    profile.skills.join(', ') : 'N/A'}</p>
            </div>
            ${profile.resumePath ? `
                <div class="profile-field">
                    <strong>Resume</strong>
                    <p><a href="#" onclick="downloadResume('${profile.resumePath.replace(/\\/g, '/')}'); return false;">
                        📄 ${profile.resumeName}
                    </a></p>
                </div>
            ` : ''}
        </div>
        ${profile.feedback ? `
            <div class="existing-feedback">
                <strong>Previous Feedback:</strong>
                <p>${profile.feedback}</p>
            </div>
        ` : ''}
    `;
    
    $('#profileContent').html(html);
    $('#feedbackInput').val(profile.feedback || '');
    $('#profileModal').fadeIn(200);
}

// Handle offer from profile modal
function handleOffer() {
    if (!currentProfileData || !currentProfileData.appId) {
        console.error('No application ID available');
        showMessage('Error', 'Application ID is missing');
        return;
    }
    
    const feedback = $('#feedbackInput').val();
    const appId = currentProfileData.appId;
    
    console.log('Offering application:', appId);
    
    $.ajax({
        url: 'moServlet',
        type: 'POST',
        data: {
            action: 'offerApplication',
            appId: appId,
            posId: currentPosId,
            feedback: feedback
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                closeProfileModal();
                updateAppItemStatus(appId, 1, feedback);
                showMessage('Success', response.message);
            } else {
                showMessage('Error', response.message);
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

// Handle reject from profile modal
function handleReject() {
    if (!currentProfileData || !currentProfileData.appId) {
        console.error('No application ID available');
        showMessage('Error', 'Application ID is missing');
        return;
    }
    
    const feedback = $('#feedbackInput').val();
    const appId = currentProfileData.appId;
    
    if (!confirm('Are you sure you want to reject this application?')) {
        return;
    }
    
    console.log('Rejecting application:', appId);
    
    $.ajax({
        url: 'moServlet',
        type: 'POST',
        data: {
            action: 'rejectApplication',
            appId: appId,
            posId: currentPosId,
            feedback: feedback
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                closeProfileModal();
                updateAppItemStatus(appId, 2, feedback);
                showMessage('Success', response.message);
            } else {
                showMessage('Error', response.message);
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

// Quick offer (from list)
function quickOffer(appId) {
    pendingAction = 'offer';
    pendingAppId = appId;
    $('#feedbackModalTitle').text('Offer Application');
    $('#quickFeedbackInput').val('');
    $('#feedbackModal').fadeIn(200);
}

// Quick reject (from list)
function quickReject(appId) {
    pendingAction = 'reject';
    pendingAppId = appId;
    $('#feedbackModalTitle').text('Reject Application');
    $('#quickFeedbackInput').val('');
    $('#feedbackModal').fadeIn(200);
}

// Confirm action from feedback modal
function confirmAction() {
    const feedback = $('#quickFeedbackInput').val();
    
    if (!pendingAppId) {
        console.error('No pending application ID');
        return;
    }
    
    if (pendingAction === 'offer') {
        $.ajax({
            url: 'moServlet',
            type: 'POST',
            data: {
                action: 'offerApplication',
                appId: pendingAppId,
                posId: currentPosId,
                feedback: feedback
            },
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    closeFeedbackModal();
                    updateAppItemStatus(pendingAppId, 1, feedback);
                    showMessage('Success', response.message);
                } else {
                    showMessage('Error', response.message);
                }
            },
            error: function(xhr) {
                const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
                showMessage('Error', errorMsg);
            }
        });
    } else if (pendingAction === 'reject') {
        $.ajax({
            url: 'moServlet',
            type: 'POST',
            data: {
                action: 'rejectApplication',
                appId: pendingAppId,
                posId: currentPosId,
                feedback: feedback
            },
            dataType: 'json',
            success: function(response) {
                if (response.success) {
                    closeFeedbackModal();
                    updateAppItemStatus(pendingAppId, 2, feedback);
                    showMessage('Success', response.message);
                } else {
                    showMessage('Error', response.message);
                }
            },
            error: function(xhr) {
                const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
                showMessage('Error', errorMsg);
            }
        });
    }
}

// Update application item status
function updateAppItemStatus(appId, status, feedback) {
    console.log('Updating app status:', appId, 'to', status);
    
    const $appItem = $(`.app-item[data-appid="${appId}"]`);
    
    if ($appItem.length === 0) {
        console.warn('App item not found for appId:', appId);
        return;
    }
    
    const statusText = status === 1 ? 'OFFERED' : 'REJECTED';
    const statusClass = status === 1 ? 'status-offered' : 'status-rejected';
    
    $appItem.find('.status-badge')
        .removeClass('status-applied status-offered status-rejected')
        .addClass(statusClass)
        .text(statusText);
    
    $appItem.find('.btn-offer-small, .btn-reject-small').remove();
    
    if (feedback) {
        const $actionsDiv = $appItem.find('.app-actions');
        if ($actionsDiv.length > 0) {
            $actionsDiv.after(`<p class="app-meta"><strong>Feedback:</strong> ${feedback}</p>`);
        } else {
            $appItem.append(`<p class="app-meta"><strong>Feedback:</strong> ${feedback}</p>`);
        }
    }
    
    console.log('App status updated successfully');
}

// Close modals
function closeProfileModal() {
    $('#profileModal').fadeOut(200);
    currentProfileData = null;
}

function closeFeedbackModal() {
    $('#feedbackModal').fadeOut(200);
    pendingAction = null;
    pendingAppId = null;
}

// Download resume
function downloadResume(filePath) {
    window.open('taServlet?action=downloadResume&file=' + encodeURIComponent(filePath), '_blank');
}

// Withdraw position from detail page
function withdrawPosition(posId) {
    if (!confirm('Are you sure you want to withdraw this position? This action cannot be undone and will delete all applications.')) {
        return;
    }

    $.ajax({
        url: 'moServlet',
        type: 'POST',
        data: {
            action: 'withdrawnPosition',
            posId: posId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                // Update status badge
                $('.detail-status .status-badge')
                    .removeClass('status-opened status-filled status-closed')
                    .addClass('status-withdrawn')
                    .text('WITHDRAWN');

                // Remove withdraw button
                $('.btn-withdraw-position').remove();

                showMessage('Success', response.message || 'Position withdrawn successfully');
            } else {
                showMessage('Error', response.message || 'Failed to withdraw position');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Failed to withdraw position';
            showMessage('Error', errorMsg);
        }
    });
}

// Helper functions
function getStatusClass(status) {
    const map = { 0: 'applied', 1: 'offered', 2: 'rejected' };
    return map[status] || 'applied';
}

function getStatusText(status) {
    const map = { 0: 'APPLIED', 1: 'OFFERED', 2: 'REJECTED' };
    return map[status] || 'APPLIED';
}

function formatDate(timestamp) {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleDateString();
}

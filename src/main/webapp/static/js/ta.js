// src/main/webapp/static/js/ta.js

$(document).ready(function() {

    // User info dropdown
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

    // Edit functionality
    $('.dropdown-item.edit').click(function() {
        alert('Edit profile - to be implemented');
    });

    // Exit functionality
    $('.dropdown-item.exit').click(function() {
        if (confirm('Are you sure you want to exit?')) {
            window.location.href = 'userServlet?action=logout';
        }
    });

    // Withdraw functionality
    $(document).on('click', '.btn-withdraw:not(:disabled)', function() {
        const appId = $(this).data('appid');
        const $card = $(this).closest('.applied-card');

        if (confirm('Are you sure you want to withdraw this application?')) {
            $.ajax({
                url: 'taServlet',
                type: 'POST',
                data: {
                    action: 'withdraw',
                    appId: appId
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        // Update status to withdrawn (3)
                        $card.find('.status-badge')
                            .removeClass('status-applied status-offered status-rejected')
                            .addClass('status-withdrawn')
                            .text('WITHDRAWN');

                        // Disable withdraw button
                        $card.find('.btn-withdraw')
                            .prop('disabled', true)
                            .text('Withdrawn');

                        showMessage('Success', response.message || 'Application withdrawn successfully');
                    } else {
                        showMessage('Error', response.message || 'Failed to withdraw application');
                    }
                },
                error: function(xhr) {
                    const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Failed to withdraw application';
                    showMessage('Error', errorMsg);
                }
            });
        }
    });

    // Withdraw functionality for detail page
    $(document).on('click', '#withdrawBtn:not(:disabled)', function() {
        const appId = $(this).data('appid');
        
        if (confirm('Are you sure you want to withdraw this application?')) {
            $.ajax({
                url: 'taServlet',
                type: 'POST',
                data: {
                    action: 'withdraw',
                    appId: appId
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        // Update status badge to withdrawn
                        $('#appStatusBadge')
                            .removeClass('status-applied status-offered status-rejected')
                            .addClass('status-withdrawn')
                            .text('WITHDRAWN');
                        
                        // Disable and hide withdraw button
                        $('#withdrawBtn')
                            .prop('disabled', true)
                            .text('Withdrawn');
                        
                        // Show success message
                        showMessage('Success', response.message || 'Application withdrawn successfully');
                        
                        // Reload page after a short delay to update the view
                        setTimeout(function() {
                            window.location.reload();
                        }, 1500);
                    } else {
                        showMessage('Error', response.message || 'Failed to withdraw application');
                    }
                },
                error: function(xhr) {
                    const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Failed to withdraw application';
                    showMessage('Error', errorMsg);
                }
            });
        }
    });

    // Filter and order change handlers for Home page
    $('#filterSelect, #orderSelect').on('change', function() {
        if ($('.applied-list').length > 0) {
            applyFiltersAndOrder('listApplied');
        }
    });

    // Filter, order, and search key change handlers for Positions page
    $('#filterSelect, #orderSelect, #searchKeySelect').on('change', function() {
        if ($('.position-list').length > 0) {
            applyFiltersAndOrder('listPositions');
        }
    });

    // Search positions functionality
    $(document).on('click', '.search-btn', function() {
        searchPositions();
    });

    $('#searchInput').keypress(function(e) {
        if (e.which === 13) {
            searchPositions();
        }
    });

    function searchPositions() {
        applyFiltersAndOrder('listPositions');
    }

    function applyFiltersAndOrder(action) {
        const filter = $('#filterSelect').val() || 'all';
        const order = $('#orderSelect').val() || (action === 'listApplied' ? 'applyAt' : 'postDate');
        const search = $('#searchInput').val() || '';
        const key = $('#searchKeySelect').val() || 'title';

        let url = 'taServlet?action=' + action + '&page=1&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order);

        if (action === 'listPositions') {
            url += '&key=' + encodeURIComponent(key) + '&search=' + encodeURIComponent(search);
        }

        window.location.href = url;
    }

    // Pagination click handler - detect page context by checking DOM elements
    $(document).on('click', '.page-item:not(.disabled,.active)', function(e) {
        e.preventDefault();
        const page = $(this).data('page');
        if (page) {
            const filter = $('#filterSelect').val() || 'all';
            const order = $('#orderSelect').val() || 'applyAt';
            const search = $('#searchInput').val() || '';
            const key = $('#searchKeySelect').val() || 'title';

            // Detect page context by checking unique elements
            if ($('.position-list').length > 0 || $('#searchInput').length > 0) {
                // Positions page
                let url = 'taServlet?action=listPositions&page=' + page + '&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order) + '&key=' + encodeURIComponent(key) + '&search=' + encodeURIComponent(search);
                window.location.href = url;
            } else if ($('.applied-list').length > 0) {
                // Applied list page (home)
                let url = 'taServlet?action=listApplied&page=' + page + '&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order);
                window.location.href = url;
            }
        }
    });

    // Show message modal
    function showMessage(title, message) {
        $('#modalTitle').text(title);
        $('#modalBody').text(message);
        $('#messageModal').css('display', 'flex');
    }

    // Close modal
    $('.close-modal').click(function() {
        $('#messageModal').fadeOut(200);
    });

    $('#confirmModal, #confirmMessageModal').click(function() {
        const $modal = $('#messageModal');
        const action = $modal.data('action');
        
        $modal.fadeOut(200);
        
        if (action === 'applySuccess') {
            const posId = $modal.data('posid');
            const appId = $modal.data('appid');
            const page = $modal.data('page');
            viewPosition(posId, appId, page);
        } else if (action === 'applyFailed') {
            const posId = $modal.data('posid');
            const page = $modal.data('page');
            viewPosition(posId, '', page);
        }
        // goToProfile action is handled in handleError function
    });
});

// Go back to previous page with filters
function goBack(from, page) {
    const filter = $('#filterSelect').val() || 'all';
    const order = $('#orderSelect').val() || (from === 'applied' ? 'applyAt' : 'postDate');
    const search = $('#searchInput').val() || '';
    const key = $('#searchKeySelect').val() || 'title';

    if (from === 'applied') {
        window.location.href = 'taServlet?action=listApplied&page=' + page + '&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order);
    } else {
        window.location.href = 'taServlet?action=listPositions&page=' + page + '&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order) + '&key=' + encodeURIComponent(key) + '&search=' + encodeURIComponent(search);
    }
}

// View application from applied list (home page)
function viewApplication(appId, posId, page) {
    const filter = $('#filterSelect').val() || 'all';
    const order = $('#orderSelect').val() || 'applyAt';
    let url = 'taServlet?action=viewPosition&posId=' + posId + '&appId=' + appId + '&page=' + page + '&from=applied&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order);
    window.location.href = url;
}

// View position details from positions list
function viewPosition(posId, appId, page) {
    const filter = $('#filterSelect').val() || 'all';
    const order = $('#orderSelect').val() || 'postDate';
    const search = $('#searchInput').val() || '';
    const key = $('#searchKeySelect').val() || 'title';

    let url = 'taServlet?action=viewPosition&posId=' + posId + '&page=' + page + '&from=positions&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order) + '&key=' + encodeURIComponent(key) + '&search=' + encodeURIComponent(search);
    if (appId && appId.trim() !== '') {
        url += '&appId=' + appId;
    }
    window.location.href = url;
}

// Apply for position
$(document).on('click', '#applyBtn:not(:disabled)', function() {
    const posId = $(this).data('posid');
    const page = $(this).data('page') || '1';
    
    if (!confirm('Are you sure you want to apply for this position?')) {
        return;
    }
    
    $.ajax({
        url: 'taServlet',
        type: 'POST',
        data: {
            action: 'apply',
            posId: posId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const $modal = $('#messageModal');
                $('#modalTitle').text('Success');
                $('#modalBody').text(response.message || 'Application submitted successfully');
                $modal.data('action', 'applySuccess');
                $modal.data('posid', posId);
                $modal.data('appid', response.data.appId);
                $modal.data('page', page);
                $modal.css('display', 'flex');
            } else {
                handleError(response.message || 'Failed to submit application', posId, page);
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error, please try again';
            handleError(errorMsg, posId, page);
        }
    });
});

/**
 * Handle apply errors with special guidance for profile completion
 */
function handleError(errorMsg, posId, page) {
    // Check if error is about incomplete profile
    if (errorMsg.toLowerCase().includes('complete your profile') || 
        errorMsg.toLowerCase().includes('profile')) {
        
        // Show custom modal with guidance
        const $modal = $('#messageModal');
        $('#modalTitle').text('Profile Required');
        $('#modalBody').html(
            '<p>You need to complete your profile before applying for positions.</p>' +
            '<p style="margin-top: 10px; color: #666;">Would you like to go to the Profile page now?</p>'
        );
        $modal.data('action', 'goToProfile');
        $modal.data('posid', posId);
        $modal.data('page', page);
        $modal.css('display', 'flex');
        
        // Override confirm button behavior
        $('#confirmMessageModal').off('click').on('click', function() {
            $modal.fadeOut(200);
            window.location.href = 'taServlet?action=getProfile';
        });
        
    } else {
        // Show regular error modal
        const $modal = $('#messageModal');
        $('#modalTitle').text('Error');
        $('#modalBody').text(errorMsg);
        $modal.data('action', 'applyFailed');
        $modal.data('posid', posId);
        $modal.data('page', page);
        $modal.css('display', 'flex');
    }
}

// Profile Page Functionality
$(document).ready(function() {
    
    // Check for success message from URL
    const urlParams = new URLSearchParams(window.location.search);
    const success = urlParams.get('success');
    
    if (success === 'created') {
        showMessage('Success', 'Profile created successfully!');
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname + window.location.search.replace(/&?success=created/, ''));
    } else if (success === 'updated') {
        showMessage('Success', 'Profile updated successfully!');
        window.history.replaceState({}, document.title, window.location.pathname + window.location.search.replace(/&?success=updated/, ''));
    }
    
    // Create Profile Button
    $('#createProfileBtn').on('click', function() {
        openProfileModal(false);
    });
    
    // Edit Profile Button
    $('#editProfileBtn').on('click', function() {
        openProfileModal(true);
    });
    
    // Close modal
    $('.close-modal').on('click', function() {
        $('#profileModal').fadeOut(200);
    });
    
    $('#cancelBtn').on('click', function() {
        $('#profileModal').fadeOut(200);
    });
    
    // Close modal when clicking outside
    $(window).on('click', function(e) {
        if ($(e.target).is('#profileModal')) {
            $('#profileModal').fadeOut(200);
        }
    });
    
    // Skills management
    $('#skillInput').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            addSkill();
        }
    });
    
    $('#addSkillBtn').on('click', function() {
        addSkill();
    });
    
    // Remove skill
    $(document).on('click', '.remove-skill', function() {
        $(this).parent('.skill-editable-tag').remove();
        updateSkillsHiddenFields();
    });
    
    // Form submission
    $('#profileForm').on('submit', function(e) {
        e.preventDefault();
        submitProfileForm();
    });
    
    // Resume file validation
    $('#resume').on('change', function() {
        validateResumeFile(this);
        
        // Show/hide extract button based on file selection
        if (this.files && this.files.length > 0) {
            $('#extractSkillsBtn').show();
        } else {
            $('#extractSkillsBtn').hide();
        }
    });
    
    // Extract skills from resume
    $('#extractSkillsBtn').on('click', function() {
        extractSkillsFromResume();
    });
});

function extractSkillsFromResume() {
    const fileInput = $('#resume')[0];
    const file = fileInput.files[0];
    
    if (!file) {
        showFormError('Please select a resume file first');
        return;
    }
    
    // Validate file type
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
        showFormError('Only PDF files are allowed');
        return;
    }
    
    // Validate file size (10MB)
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
        showFormError('File size must be less than 10MB');
        return;
    }
    
    // Show loading status
    $('#extractStatus')
        .removeClass('alert-danger alert-success')
        .addClass('alert-info')
        .text('Extracting skills from resume... Please wait.')
        .show();
    
    $('#extractSkillsBtn').prop('disabled', true).text('Extracting...');
    
    // Create FormData and send via AJAX
    const formData = new FormData();
    formData.append('action', 'extractSkills');
    formData.append('resume', file);
    
    $.ajax({
        url: 'taServlet',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(response) {
            if (response.success && response.data && response.data.skills) {
                const skills = response.data.skills;
                
                if (skills.length === 0) {
                    showExtractStatus('No skills found in the resume', 'warning');
                } else {
                    // Add extracted skills to the list
                    let addedCount = 0;
                    let duplicateCount = 0;
                    
                    skills.forEach(function(skill) {
                        if (addSkillIfNotExists(skill)) {
                            addedCount++;
                        } else {
                            duplicateCount++;
                        }
                    });
                    
                    updateSkillsHiddenFields();
                    
                    const message = `Successfully extracted ${addedCount} skill(s)` + 
                                   (duplicateCount > 0 ? ` (${duplicateCount} duplicates skipped)` : '');
                    showExtractStatus(message, 'success');
                }
            } else {
                showExtractStatus(response.message || 'Failed to extract skills', 'error');
            }
            
            $('#extractSkillsBtn').prop('disabled', false).text('Extract Skills');
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error occurred';
            showExtractStatus(errorMsg, 'error');
            $('#extractSkillsBtn').prop('disabled', false).text('Extract Skills');
        }
    });
}

function addSkillIfNotExists(skill) {
    if (!skill || skill.trim() === '') {
        return false;
    }
    
    skill = skill.trim();
    
    // Check for duplicates (case-insensitive)
    let exists = false;
    $('.skill-editable-tag .skill-text').each(function() {
        if ($(this).text().toLowerCase() === skill.toLowerCase()) {
            exists = true;
            return false; // break loop
        }
    });
    
    if (exists) {
        return false;
    }
    
    addSkillTag(skill);
    return true;
}

function showExtractStatus(message, type) {
    const $status = $('#extractStatus');
    $status.removeClass('alert-info alert-success alert-danger alert-warning');
    
    switch(type) {
        case 'success':
            $status.addClass('alert-success');
            break;
        case 'error':
            $status.addClass('alert-danger');
            break;
        case 'warning':
            $status.addClass('alert-warning');
            break;
        default:
            $status.addClass('alert-info');
    }
    
    $status.text(message).show();
    
    // Auto-hide after 5 seconds for success messages
    if (type === 'success') {
        setTimeout(function() {
            $status.fadeOut(300);
        }, 5000);
    }
}

function openProfileModal(isEdit) {
    window.isEditMode = isEdit;
    
    if (isEdit) {
        $('#modalTitle').text('Edit Profile');
        $('#profileForm input[name="action"]').val('updateProfile');
        $('#resumeRequired').hide();
        $('#currentResume').show();
        
        // Read data from button data attributes
        const $btn = $('#editProfileBtn');
        $('#currentResumeName').text($btn.data('resume-name'));
        $('#resumeHint').text('Leave empty to keep current resume');
        
        // Load existing data
        loadExistingProfile();
    } else {
        $('#modalTitle').text('Create Profile');
        $('#profileForm input[name="action"]').val('createProfile');
        $('#resumeRequired').show();
        $('#currentResume').hide();
        $('#resumeHint').text('Only PDF files are accepted');
        
        // Clear form
        $('#profileForm')[0].reset();
        $('#skillsContainer .skills-tags').remove();
        $('#skillsHiddenFields').empty();
    }
    
    $('#formError').hide();
    $('#profileModal').css('display', 'flex');
}

function loadExistingProfile() {
    const $btn = $('#editProfileBtn');
    
    $('#name').val($btn.data('name') || '');
    $('#gender').val($btn.data('gender') || '');
    $('#age').val($btn.data('age') || '');
    $('#college').val($btn.data('college') || '');
    $('#major').val($btn.data('major') || '');
    $('#grade').val($btn.data('grade') || '');
    $('#email').val($btn.data('email') || '');
    $('#phone').val($btn.data('phone') || '');
    
    const skills = [];
    $('.profile-skills-data .skill-data').each(function() {
        skills.push($(this).text());
    });
    
    if (skills.length > 0) {
        skills.forEach(function(skill) {
            addSkillTag(skill);
        });
        updateSkillsHiddenFields();
    }
}

function addSkill() {
    const skill = $('#skillInput').val().trim();
    
    if (!skill) {
        showFormError('Please enter a skill');
        return;
    }
    
    // Check for duplicates
    const existingSkills = [];
    $('.skill-editable-tag').each(function() {
        existingSkills.push($(this).find('.skill-text').text());
    });
    
    if (existingSkills.includes(skill)) {
        showFormError('Skill already added');
        return;
    }
    
    addSkillTag(skill);
    $('#skillInput').val('');
    updateSkillsHiddenFields();
}

function addSkillTag(skill) {
    const tagHtml = `
        <span class="skill-editable-tag">
            <span class="skill-text">${escapeHtml(skill)}</span>
            <button type="button" class="remove-skill">×</button>
        </span>
    `;
    
    let $tagsContainer = $('#skillsContainer .skills-tags');
    if ($tagsContainer.length === 0) {
        $tagsContainer = $('<div class="skills-tags"></div>');
        $('#skillsContainer').append($tagsContainer);
    }
    
    $tagsContainer.append(tagHtml);
}

function updateSkillsHiddenFields() {
    $('#skillsHiddenFields').empty();
    
    $('.skill-editable-tag .skill-text').each(function() {
        const skill = $(this).text();
        $('<input>').attr({
            type: 'hidden',
            name: 'skills',
            value: skill
        }).appendTo('#skillsHiddenFields');
    });
}

function validateResumeFile(input) {
    const file = input.files[0];
    if (!file) return true;
    
    // Check file type
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
        showFormError('Only PDF files are allowed');
        $(input).val('');
        return false;
    }
    
    // Check file size (10MB)
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
        showFormError('File size must be less than 10MB');
        $(input).val('');
        return false;
    }
    
    hideFormError();
    return true;
}

function submitProfileForm() {
    const formData = new FormData($('#profileForm')[0]);
    
    // Validate required fields
    const name = $('#name').val().trim();
    if (!name) {
        showFormError('Name is required');
        return;
    }
    
    if (!window.isEditMode) {
        const resume = $('#resume')[0].files[0];
        if (!resume) {
            showFormError('Resume is required when creating profile');
            return;
        }
    }
    
    const resumeFile = $('#resume')[0].files[0];
    if (resumeFile && !validateResumeFile($('#resume')[0])) {
        return;
    }
    
    // Disable submit button
    $('#submitBtn').prop('disabled', true).text('Saving...');
    
    $.ajax({
        url: 'taServlet',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(response) {
            // Redirect will happen on server side
            window.location.reload();
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Failed to save profile';
            showFormError(errorMsg);
            $('#submitBtn').prop('disabled', false).text('Save');
        }
    });
}

function showFormError(message) {
    $('#formError').text(message).show();
}

function hideFormError() {
    $('#formError').hide();
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function showMessage(title, message) {
    $('#messageModalTitle').text(title);
    $('#messageModalBody').text(message);
    $('#messageModal').css('display', 'flex');
}

$('#confirmMessageModal').on('click', function() {
    $('#messageModal').fadeOut(200);
});

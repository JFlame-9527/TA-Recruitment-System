$(document).ready(function() {
    const $userDropdown = $('.user-dropdown');

    $('.user-info').click(function(e) {
        e.stopPropagation();
        $userDropdown.toggleClass('show');
    });

    $(document).click(function() {
        $userDropdown.removeClass('show');
    });

    $userDropdown.click(function(e) {
        e.stopPropagation();
    });

    $('.dropdown-item.exit').click(function() {
        if (confirm('Are you sure you want to exit?')) {
            window.location.href = 'userServlet?action=logout';
        }
    });

    $('.dropdown-item.edit').click(function() {
        openEditCurrentUserModal();
    });

    $(document).on('click', '.close-modal', function() {
        $(this).closest('.modal').fadeOut(200);
    });

    $(window).click(function(e) {
        if ($(e.target).hasClass('modal')) {
            $(e.target).fadeOut(200);
        }
    });

    $('#confirmMessageModal').click(function() {
        $('#messageModal').fadeOut(200);
    });

    // Filter and order change handlers
    $('#filterSelect, #orderSelect').on('change', function() {
        applyFiltersAndOrder();
    });

    $(document).on('click', '.page-item:not(.active)', function(e) {
        e.preventDefault();
        const $pagination = $(this).closest('.pagination');
        const role = $pagination.data('role');
        const page = $(this).data('page');
        if (page) {
            loadUserList(role, page);
        }
    });

    if ($('#editTaForm').length > 0) {
        $('#editTaForm').submit(function(e) {
            e.preventDefault();
            submitEditTaForm();
        });
    }

    if ($('#editMoForm').length > 0) {
        $('#editMoForm').submit(function(e) {
            e.preventDefault();
            submitEditMoForm();
        });
    }

    if ($('#createMoForm').length > 0) {
        $('#createMoForm').submit(function(e) {
            e.preventDefault();
            submitCreateMoForm();
        });
    }
});

function switchTab(role) {
    $('.tab-btn').removeClass('active');
    $(`.tab-btn[data-role="${role}"]`).addClass('active');

    $('.user-list-container').removeClass('active').hide();

    if (role === 1) {
        $('#taListContainer').addClass('active').show();
    } else {
        $('#moListContainer').addClass('active').show();
    }
}

function applyFiltersAndOrder() {
    const filter = $('#filterSelect').val() || 'all';
    const order = $('#orderSelect').val() || 'name';
    
    const activeRole = $('.tab-btn.active').data('role') || 1;
    
    let url = 'adminServlet?action=listAccounts&page=1&filter=' + encodeURIComponent(filter) + '&order=' + encodeURIComponent(order);
    
    window.location.href = url;
}

function loadUserList(role, page) {
    const filter = $('#filterSelect').val() || 'all';
    const order = $('#orderSelect').val() || 'name';
    
    $.ajax({
        url: 'adminServlet',
        data: {
            action: 'loadAccountsPage',
            role: role,
            page: page,
            filter: filter,
            order: order
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                renderUserList(response.data, role);
            } else {
                showMessage('Error', response.message || 'Failed to load users');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

function renderUserList(data, role) {
    const accounts = data.accounts;
    const currentPage = data.currentPage;
    const totalPages = data.totalPages;

    const containerId = role === 1 ? '#taListContainer' : '#moListContainer';
    const $container = $(containerId);

    if (!accounts || accounts.length === 0) {
        $container.html(`
            <div class="empty-state">
                <div class="empty-icon">👥</div>
                <div class="empty-text">No ${role === 1 ? 'TA' : 'MO'} accounts found</div>
            </div>
        `);
        return;
    }

    let html = '<div class="user-list">';
    accounts.forEach(user => {
        const statusText = user.status === 0 ? 'Available' : 'Frozen';
        const statusClass = user.status === 0 ? 'status-available' : 'status-frozen';
        const toggleText = user.status === 0 ? 'Disable' : 'Enable';
        const roleClass = role === 1 ? 'role-ta' : 'role-mo';
        const roleLabel = role === 1 ? 'TA' : 'MO';
        const shortId = user.userId.substring(0, 8);
        const createTime = formatDate(user.createAt);

        html += `
            <div class="user-card" data-userid="${user.userId}" data-role="${role}">
                <div class="user-info-section">
                    <span class="user-id">#${shortId}</span>
                    <span class="username">${escapeHtml(user.name)}</span>
                    <span class="role-badge ${roleClass}">${roleLabel}</span>
                    <span class="status-badge ${statusClass}">${statusText}</span>
                    <span class="time-info">Created: ${createTime}</span>
                </div>

                <div class="user-actions">
                    <button class="btn btn-view" onclick="viewProfile('${user.userId}', ${role})">View Profile</button>
                    <button class="btn btn-toggle" onclick="toggleStatus('${user.userId}', ${user.status})">${toggleText}</button>
                    <button class="btn btn-edit" onclick="editUser('${user.userId}', ${role})">Edit</button>
                    <button class="btn btn-delete" onclick="deleteUser('${user.userId}')">Delete</button>
                </div>
            </div>
        `;
    });
    html += '</div>';

    html += renderPagination(currentPage, totalPages, role);

    $container.html(html);
}

function renderPagination(currentPage, totalPages, role) {
    if (totalPages <= 1) return '';

    let html = `<div class="pagination" data-role="${role}"><`;

    if (currentPage > 1) {
        html += `<a class="page-item" data-page="${currentPage - 1}">&laquo;</a>`;
    }

    for (let i = 1; i <= totalPages; i++) {
        const activeClass = i === currentPage ? 'active' : '';
        html += `<a class="page-item ${activeClass}" data-page="${i}">${i}</a>`;
    }

    if (currentPage < totalPages) {
        html += `<a class="page-item" data-page="${currentPage + 1}">&raquo;</a>`;
    }

    html += '</div>';
    return html;
}

function viewProfile(userId, role) {
    if (role === 1) {
        viewTaProfile(userId);
    } else {
        viewMoProfile(userId);
    }
}

function viewTaProfile(userId) {
    $.ajax({
        url: 'adminServlet',
        data: {
            action: 'getTAProfile',
            userId: userId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                showTaProfileModal(response.data);
            } else {
                showMessage('Error', response.message || 'Failed to load TA profile');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

function showTaProfileModal(profile) {
    const skillsHtml = profile.skills && profile.skills.length > 0
        ? profile.skills.map(skill => `<span class="skill-tag">${escapeHtml(skill)}</span>`).join('')
        : '<p style="color: #999;">No skills listed</p>';

    const resumeHtml = profile.resumePath
        ? `
            <div class="resume-section">
                <strong>Resume</strong>
                <p>${escapeHtml(profile.resumeName)}</p>
                <div class="resume-actions">
                    <a href="taServlet?action=downloadResume&file=${encodeURIComponent(profile.resumePath)}" 
                       target="_blank" class="btn btn-secondary">View</a>
                    <a href="taServlet?action=downloadResume&file=${encodeURIComponent(profile.resumePath)}&download=true" 
                       class="btn btn-secondary">Download</a>
                </div>
            </div>
          `
        : '';

    const html = `
        <div class="profile-detail">
            <div class="profile-field">
                <strong>Name</strong>
                <p>${escapeHtml(profile.name || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Gender</strong>
                <p>${escapeHtml(profile.gender || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Age</strong>
                <p>${escapeHtml(profile.age || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>College</strong>
                <p>${escapeHtml(profile.college || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Major</strong>
                <p>${escapeHtml(profile.major || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Grade</strong>
                <p>${escapeHtml(profile.grade || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Email</strong>
                <p>${escapeHtml(profile.email || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Phone</strong>
                <p>${escapeHtml(profile.phone || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Skills</strong>
                <div class="skills-list">${skillsHtml}</div>
            </div>
            ${resumeHtml}
            <div class="profile-field">
                <strong>Created At</strong>
                <p>${formatDate(profile.createAt)}</p>
            </div>
            <div class="profile-field">
                <strong>Updated At</strong>
                <p>${formatDate(profile.updateAt)}</p>
            </div>
        </div>
    `;

    $('#taProfileContent').html(html);
    $('#viewTaProfileModal').css('display', 'flex');
}

function viewMoProfile(userId) {
    $.ajax({
        url: 'adminServlet',
        data: {
            action: 'getMOProfile',
            userId: userId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                showMoProfileModal(response.data);
            } else {
                showMessage('Error', response.message || 'Failed to load MO profile');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

function showMoProfileModal(profile) {
    const html = `
        <div class="profile-detail">
            <div class="profile-field">
                <strong>Name</strong>
                <p>${escapeHtml(profile.name || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>College</strong>
                <p>${escapeHtml(profile.college || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Email</strong>
                <p>${escapeHtml(profile.email || 'N/A')}</p>
            </div>
            <div class="profile-field">
                <strong>Phone</strong>
                <p>${escapeHtml(profile.phone || 'N/A')}</p>
            </div>
        </div>
    `;

    $('#moProfileContent').html(html);
    $('#viewMoProfileModal').css('display', 'flex');
}

function toggleStatus(userId, currentStatus) {
    const newStatus = currentStatus === 0 ? 1 : 0;
    const actionText = newStatus === 0 ? 'enable' : 'disable';

    if (!confirm(`Are you sure you want to ${actionText} this user?`)) {
        return;
    }

    $.ajax({
        url: 'adminServlet',
        type: 'POST',
        data: {
            action: 'updateStatus',
            userId: userId,
            status: newStatus
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const $card = $(`.user-card[data-userid="${userId}"]`);
                const statusText = newStatus === 0 ? 'Available' : 'Frozen';
                const statusClass = newStatus === 0 ? 'status-available' : 'status-frozen';
                const toggleText = newStatus === 0 ? 'Disable' : 'Enable';

                $card.find('.status-badge')
                    .removeClass('status-available status-frozen')
                    .addClass(statusClass)
                    .text(statusText);

                $card.find('.btn-toggle')
                    .text(toggleText)
                    .attr('onclick', `toggleStatus('${userId}', ${newStatus})`);

                showMessage('Success', response.message);
            } else {
                showMessage('Error', response.message || 'Failed to update status');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

function editUser(userId, role) {
    if (role === 1) {
        openEditTaModal(userId);
    } else {
        openEditMoModal(userId);
    }
}

function openEditTaModal(userId) {
    $('#editTaUserId').val(userId);
    $('#editTaUsername').val('');
    $('#editTaPassword').val('');
    $('#editTaError').hide();

    const $card = $(`.user-card[data-userid="${userId}"]`);
    const username = $card.find('.username').text();
    $('#editTaUsername').val(username);

    $('#editTaUserModal').css('display', 'flex');
}

function submitEditTaForm() {
    const userId = $('#editTaUserId').val();
    const username = $('#editTaUsername').val().trim();
    const newPassword = $('#editTaPassword').val();

    if (!username) {
        $('#editTaError').text('Username is required').show();
        return;
    }

    if (newPassword && newPassword.length < 6) {
        $('#editTaError').text('Password must be at least 6 characters').show();
        return;
    }

    const requests = [];

    requests.push(
        $.ajax({
            url: 'adminServlet',
            type: 'POST',
            data: {
                action: 'updateUser',
                id: userId,
                name: username,
                password: newPassword
            },
            dataType: 'json'
        })
    );

    Promise.all(requests)
        .then(() => {
            closeModal('editTaUserModal');
            showMessage('Success', 'TA account updated successfully');
            location.reload();
        })
        .catch((error) => {
            const errorMsg = error.responseJSON ? error.responseJSON.message : 'Failed to update account';
            $('#editTaError').text(errorMsg).show();
        });
}

function openEditMoModal(userId) {
    $('#editMoUserId').val(userId);
    $('#editMoProfileId').val('');
    $('#editMoUsername').val('');
    $('#editMoPassword').val('');
    $('#editMoName').val('');
    $('#editMoCollege').val('');
    $('#editMoEmail').val('');
    $('#editMoPhone').val('');
    $('#editMoError').hide();

    const $card = $(`.user-card[data-userid="${userId}"]`);
    const username = $card.find('.username').text();
    $('#editMoUsername').val(username);

    $.ajax({
        url: 'adminServlet',
        data: {
            action: 'getMOProfile',
            userId: userId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                const profile = response.data;
                $('#editMoProfileId').val(profile.id);
                $('#editMoName').val(profile.name || '');
                $('#editMoCollege').val(profile.college || '');
                $('#editMoEmail').val(profile.email || '');
                $('#editMoPhone').val(profile.phone || '');
            }
        },
        error: function() {
            console.log('MO profile not found, will create new one if needed');
        }
    });

    $('#editMoAccountModal').css('display', 'flex');
}

function submitEditMoForm() {
    const userId = $('#editMoUserId').val();
    const profileId = $('#editMoProfileId').val();
    const username = $('#editMoUsername').val().trim();
    const newPassword = $('#editMoPassword').val();
    const name = $('#editMoName').val().trim();
    const college = $('#editMoCollege').val().trim();
    const email = $('#editMoEmail').val().trim();
    const phone = $('#editMoPhone').val().trim();

    if (!username) {
        $('#editMoError').text('Username is required').show();
        return;
    }

    if (newPassword && newPassword.length < 6) {
        $('#editMoError').text('Password must be at least 6 characters').show();
        return;
    }

    const requests = [];

    requests.push(
        $.ajax({
            url: 'adminServlet',
            type: 'POST',
            data: {
                action: 'updateUser',
                id: userId,
                name: username,
                password: newPassword
            },
            dataType: 'json'
        })
    );

    if (profileId) {
        requests.push(
            $.ajax({
                url: 'adminServlet',
                type: 'POST',
                data: {
                    action: 'updateMOProfile',
                    id: profileId,
                    userId: userId,
                    name: name,
                    college: college,
                    email: email,
                    phone: phone
                },
                dataType: 'json'
            })
        );
    }

    Promise.all(requests)
        .then(() => {
            closeModal('editMoAccountModal');
            showMessage('Success', 'MO account updated successfully');
            location.reload();
        })
        .catch((error) => {
            const errorMsg = error.responseJSON ? error.responseJSON.message : 'Failed to update account';
            $('#editMoError').text(errorMsg).show();
        });
}

function deleteUser(userId) {
    const $card = $(`.user-card[data-userid="${userId}"]`);
    const username = $card.find('.username').text();

    if (!confirm(`Are you sure you want to delete user ${username}? This will also delete all associated data (applications, profiles, positions).`)) {
        return;
    }

    $.ajax({
        url: 'adminServlet',
        type: 'POST',
        data: {
            action: 'deleteUser',
            userId: userId
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                $card.fadeOut(300, function() {
                    $(this).remove();
                });
                showMessage('Success', response.message);
            } else {
                showMessage('Error', response.message || 'Failed to delete user');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            showMessage('Error', errorMsg);
        }
    });
}

function closeModal(modalId) {
    $(`#${modalId}`).fadeOut(200);
}

function showMessage(title, message) {
    $('#messageModalTitle').text(title);
    $('#messageModalBody').text(message);
    $('#messageModal').css('display', 'flex');
}

function formatDate(timestamp) {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function submitCreateMoForm() {
    const formData = {
        username: $('#username').val().trim(),
        password: $('#password').val(),
        name: $('#name').val().trim(),
        college: $('#college').val().trim(),
        email: $('#email').val().trim(),
        phone: $('#phone').val().trim()
    };

    if (!validateCreateMoForm(formData)) {
        return;
    }

    $.ajax({
        url: 'adminServlet',
        type: 'POST',
        data: {
            action: 'createMOAccount',
            username: formData.username,
            password: formData.password,
            name: formData.name,
            college: formData.college,
            email: formData.email,
            phone: formData.phone
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                showCreateMoSuccess('MO account created successfully!');
                $('#createMoForm')[0].reset();
            } else {
                showCreateMoError(response.message || 'Failed to create MO account');
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error occurred';
            showCreateMoError(errorMsg);
        }
    });
}

function validateCreateMoForm(data) {
    if (!data.username) {
        showCreateMoError('Username is required');
        return false;
    }

    if (!data.password) {
        showCreateMoError('Password is required');
        return false;
    }

    if (data.password.length < 6) {
        showCreateMoError('Password must be at least 6 characters');
        return false;
    }

    if (!data.name) {
        showCreateMoError('Full name is required');
        return false;
    }

    if (data.email && !isValidEmail(data.email)) {
        showCreateMoError('Please enter a valid email address');
        return false;
    }

    return true;
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function showCreateMoError(message) {
    $('#formError').text(message).show();
    $('#formSuccess').hide();
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}

function showCreateMoSuccess(message) {
    $('#formSuccess').text(message).show();
    $('#formError').hide();
    $('html, body').animate({ scrollTop: 0 }, 'fast');
}

function openEditCurrentUserModal() {
    const $userInfo = $('.user-info');
    const userId = $userInfo.data('userid');
    const username = $userInfo.data('username');

    console.log('Current user ID:', userId);
    console.log('Current username:', username);

    if (!userId || !username) {
        showMessage('Error', 'Failed to load user information');
        return;
    }

    $('#editCurrentUserUserId').val(userId);
    $('#editCurrentUserUsername').val(username);
    $('#editCurrentUserPassword').val('');
    $('#editCurrentUserError').hide();

    $('#editCurrentUserModal').css('display', 'flex');
}

function submitEditCurrentUserForm() {
    const userId = $('#editCurrentUserUserId').val();
    const username = $('#editCurrentUserUsername').val().trim();
    const newPassword = $('#editCurrentUserPassword').val();

    if (!username) {
        $('#editCurrentUserError').text('Username is required').show();
        return;
    }

    if (newPassword && newPassword.length < 6) {
        $('#editCurrentUserError').text('Password must be at least 6 characters').show();
        return;
    }

    $.ajax({
        url: 'userServlet',
        type: 'POST',
        data: {
            action: 'modifyUser',
            userId: userId,
            username: username,
            password: newPassword
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                closeModal('editCurrentUserModal');
                showMessage('Success', 'Account updated successfully. Please refresh the page.');
                setTimeout(function() {
                    location.reload();
                }, 1500);
            } else {
                $('#editCurrentUserError').text(response.message || 'Failed to update account').show();
            }
        },
        error: function(xhr) {
            const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : 'Network error';
            $('#editCurrentUserError').text(errorMsg).show();
        }
    });
}

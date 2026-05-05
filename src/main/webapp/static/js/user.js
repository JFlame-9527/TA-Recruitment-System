$(document).ready(function() {
    // Define modal elements
    const $messageModal = $('#messageModal');
    const $modalTitle = $('#modalTitle');
    const $modalBody = $('#modalBody');
    const $closeBtn = $('.close-modal');
    const $confirmBtn = $('#confirmModal');
    
    // Register modal elements
    const $registerModal = $('#registerModal');
    const $registerForm = $('#registerForm');
    const $registerError = $('#registerError');
    const $regUsername = $('#reg_username');
    const $regPassword = $('#reg_password');
    const $regCheckPassword = $('#reg_checkpassword');

    // Show message modal function
    function showMessageModal(title, message) {
        $modalTitle.text(title);
        $modalBody.text(message);
        $messageModal.css('display', 'flex');
    }

    // Hide message modal function
    function hideMessageModal() {
        $messageModal.fadeOut(200);
    }

    // Show register modal
    function showRegisterModal() {
        $registerError.hide();
        $registerForm[0].reset();
        $registerModal.css('display', 'flex');
    }

    // Hide register modal
    function hideRegisterModal() {
        $registerModal.fadeOut(200);
    }

    // Show register error
    function showRegisterError(message) {
        $registerError.text(message).show();
    }

    // Click "Forgot Password"
    $('#forgotPwd').on('click', function(e) {
        e.preventDefault();
        showMessageModal('Reset Password', 'Please contact administrator to reset password');
    });

    // Click "Register Account" - open register modal
    $('#registerLink').on('click', function(e) {
        e.preventDefault();
        showRegisterModal();
    });

    // Register form submission
    $registerForm.on('submit', async function(e) {
        e.preventDefault();
        
        const username = $regUsername.val().trim();
        const password = $regPassword.val();
        const checkPassword = $regCheckPassword.val();
        
        // Validate password match
        if (password !== checkPassword) {
            showRegisterError('Passwords do not match');
            return;
        }
        
        // Validate password length
        if (password.length < 6) {
            showRegisterError('Password must be at least 6 characters');
            return;
        }
        
        try {
            // Step 1: Check if username exists
            const checkResponse = await $.ajax({
                url: 'userServlet',
                type: 'POST',
                data: {
                    action: 'checkUsername',
                    username: username
                },
                dataType: 'json'
            });
            
            if (!checkResponse.success) {
                showRegisterError(checkResponse.message || 'Username already exists');
                return;
            }
            
            // Step 2: Execute registration
            const registerResponse = await $.ajax({
                url: 'userServlet',
                type: 'POST',
                data: {
                    action: 'register',
                    username: username,
                    password: password
                },
                dataType: 'json'
            });
            
            if (registerResponse.success) {
                hideRegisterModal();
                showMessageModal('Success', registerResponse.message || 'Account created successfully! Please login.');
                
                // Clear form
                $registerForm[0].reset();
            } else {
                showRegisterError(registerResponse.message || 'Registration failed');
            }
            
        } catch (error) {
            console.error('Registration error:', error);
            const errorMsg = error.responseJSON ? error.responseJSON.message : 'Network error, please try again';
            showRegisterError(errorMsg);
        }
    });

    // Close register modal
    $registerModal.find('.close-modal').on('click', hideRegisterModal);
    
    // Click register modal background to close
    $registerModal.on('click', function(e) {
        if ($(e.target).is($registerModal)) {
            hideRegisterModal();
        }
    });

    // Click message modal close button
    $closeBtn.on('click', hideMessageModal);

    // Click message modal confirm button
    $confirmBtn.on('click', hideMessageModal);

    // Click message modal background to close
    $messageModal.on('click', function(e) {
        if ($(e.target).is($messageModal)) {
            hideMessageModal();
        }
    });

    // Simple validation or animation before form submission (optional)
    $('#loginForm').on('submit', async function(e) {
        e.preventDefault();
        
        const username = $('#username').val().trim();
        const password = $('#password').val();
        const $btn = $('#btn-login.btn-login');
        const originalText = $btn.text();
        
        if(!username || !password) {
            showMessageModal('Error', 'Please enter username and password');
            return false;
        }
        
        $btn.text('Logging in...').prop('disabled', true);
        
        try {
            const response = await $.ajax({
                url: 'userServlet',
                type: 'POST',
                data: {
                    action: 'login',
                    username: username,
                    password: password
                },
                dataType: 'json'
            });
            
            if (response.success) {
                setTimeout(() => {
                    window.location.href = response.data.redirectUrl;
                }, 500);
            } else{
                showMessageModal('Login Failed', response.message || 'Invalid username or password');
                $btn.text(originalText).prop('disabled', false);
                $('#password').val('');
            }
            
        } catch (error) {
            console.error('Login error:', error);
            let errorMsg = 'Network error, please try again';
            
            if (error.status === 401) {
                errorMsg = 'Invalid username or password';
            } else if (error.status === 403) {
                errorMsg = error.responseJSON ? error.responseJSON.message : 'Account is frozen. Please contact administrator.';
            } else if (error.responseJSON && error.responseJSON.message) {
                errorMsg = error.responseJSON.message;
            }
            
            showMessageModal('Login Failed', errorMsg);
            $btn.text(originalText).prop('disabled', false);
            $('#password').val('');
        }
    });
});
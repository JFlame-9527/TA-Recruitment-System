$(document).ready(function() {
    const $messageModal = $('#messageModal');
    const $modalTitle = $('#modalTitle');
    const $modalBody = $('#modalBody');
    const $closeBtn = $('.close-modal');
    const $confirmBtn = $('#confirmModal');
    
    const $registerModal = $('#registerModal');
    const $registerForm = $('#registerForm');
    const $registerError = $('#registerError');
    const $regUsername = $('#reg_username');
    const $regPassword = $('#reg_password');
    const $regCheckPassword = $('#reg_checkpassword');
    
    const $captchaGroup = $('#captchaGroup');
    const $captchaInput = $('#captcha');
    const $captchaImg = $('#captchaImg');

    let requireCaptcha = false;

    function showMessageModal(title, message) {
        $modalTitle.text(title);
        $modalBody.text(message);
        $messageModal.css('display', 'flex');
    }

    function hideMessageModal() {
        $messageModal.fadeOut(200);
    }

    function showRegisterModal() {
        $registerError.hide();
        $registerForm[0].reset();
        $registerModal.css('display', 'flex');
    }

    function hideRegisterModal() {
        $registerModal.fadeOut(200);
    }

    function showRegisterError(message) {
        $registerError.text(message).show();
    }

    function showCaptcha() {
        if (!requireCaptcha) {
            requireCaptcha = true;
            $captchaGroup.fadeIn(300);
            refreshCaptcha();
        }
    }

    function hideCaptcha() {
        requireCaptcha = false;
        $captchaGroup.hide();
        $captchaInput.val('');
    }

    function refreshCaptcha() {
        $captchaImg.attr('src', 'captcha?' + new Date().getTime());
        $captchaInput.val('');
        $captchaInput.focus();
    }

    async function checkCaptchaStatus() {
        try {
            const response = await $.ajax({
                url: 'userServlet',
                type: 'POST',
                data: {
                    action: 'getCaptchaStatus'
                },
                dataType: 'json'
            });
            
            if (response.success && response.data.requireCaptcha) {
                showCaptcha();
            }
        } catch (error) {
            console.error('Check captcha status error:', error);
        }
    }

    $('#forgotPwd').on('click', function(e) {
        e.preventDefault();
        showMessageModal('Reset Password', 'Please contact administrator to reset password');
    });

    $('#registerLink').on('click', function(e) {
        e.preventDefault();
        showRegisterModal();
    });

    $registerForm.on('submit', async function(e) {
        e.preventDefault();
        
        const username = $regUsername.val().trim();
        const password = $regPassword.val();
        const checkPassword = $regCheckPassword.val();
        
        if (password !== checkPassword) {
            showRegisterError('Passwords do not match');
            return;
        }
        
        if (password.length < 6) {
            showRegisterError('Password must be at least 6 characters');
            return;
        }
        
        try {
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

    $registerModal.find('.close-modal').on('click', hideRegisterModal);
    
    $registerModal.on('click', function(e) {
        if ($(e.target).is($registerModal)) {
            hideRegisterModal();
        }
    });

    $closeBtn.on('click', hideMessageModal);

    $confirmBtn.on('click', hideMessageModal);

    $messageModal.on('click', function(e) {
        if ($(e.target).is($messageModal)) {
            hideMessageModal();
        }
    });

    $captchaImg.on('click', function(e) {
        e.preventDefault();
        refreshCaptcha();
    });

    $('#loginForm').on('submit', async function(e) {
        e.preventDefault();
        
        const username = $('#username').val().trim();
        const password = $('#password').val();
        const captcha = $captchaInput.val().trim();
        const $btn = $('#btn-login.btn-login');
        const originalText = $btn.text();
        
        if(!username || !password) {
            showMessageModal('Error', 'Please enter username and password');
            return false;
        }
        
        if (requireCaptcha && !captcha) {
            showMessageModal('Error', 'Please enter verification code');
            return false;
        }
        
        $btn.text('Logging in...').prop('disabled', true);
        
        try {
            const requestData = {
                action: 'login',
                username: username,
                password: password
            };
            
            if (requireCaptcha) {
                requestData.captcha = captcha;
            }
            
            const response = await $.ajax({
                url: 'userServlet',
                type: 'POST',
                data: requestData,
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
                
                if (response.data && response.data.requireCaptcha) {
                    showCaptcha();
                }
                
                if (requireCaptcha) {
                    refreshCaptcha();
                }
            }
            
        } catch (error) {
            console.error('Login error:', error);
            let errorMsg = 'Network error, please try again';
            
            if (error.status === 401) {
                errorMsg = error.responseJSON ? error.responseJSON.message : 'Invalid username or password';
                if (error.responseJSON && error.responseJSON.data && error.responseJSON.data.requireCaptcha) {
                    showCaptcha();
                }
            } else if (error.status === 403) {
                errorMsg = error.responseJSON ? error.responseJSON.message : 'Account is frozen. Please contact administrator.';
            } else if (error.status === 400) {
                errorMsg = error.responseJSON ? error.responseJSON.message : 'Invalid verification code';
                if (requireCaptcha) {
                    refreshCaptcha();
                }
            } else if (error.responseJSON && error.responseJSON.message) {
                errorMsg = error.responseJSON.message;
            }
            
            showMessageModal('Login Failed', errorMsg);
            $btn.text(originalText).prop('disabled', false);
            $('#password').val('');
            
            if (requireCaptcha) {
                refreshCaptcha();
            }
        }
    });

    checkCaptchaStatus();
});
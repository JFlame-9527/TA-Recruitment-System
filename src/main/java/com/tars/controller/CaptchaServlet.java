package com.tars.controller;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.util.Config;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Properties;

/**
 * Servlet for generating CAPTCHA (Completely Automated Public Turing test to tell Computers and Humans Apart) images.
 * <p>
 * This servlet generates visual challenge images to prevent automated bot attacks on the login form.
 * The CAPTCHA is displayed as an image and the correct answer is stored in the user's session
 * for later verification during login.
 * </p>
 * <p>
 * <b>CAPTCHA Configuration:</b>
 * <ul>
 *   <li><b>Image Size</b>: 150x50 pixels</li>
 *   <li><b>Character Set</b>: Digits (0-9) and uppercase letters (A-Z)</li>
 *   <li><b>Length</b>: 4 characters</li>
 *   <li><b>Font Size</b>: 40pt</li>
 *   <li><b>Noise</b>: Disabled (NoNoise implementation)</li>
 *   <li><b>Effect</b>: Water ripple distortion for added security</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage:</b> Include in HTML as an image source:
 * <pre>{@code
 * <img src="captcha" alt="CAPTCHA" id="captchaImg" onclick="refreshCaptcha()">
 * 
 * <script>
 * function refreshCaptcha() {
 *     document.getElementById('captchaImg').src = 'captcha?' + Math.random();
 * }
 * </script>
 * }</pre>
 * </p>
 * <p>
 * <b>Session Storage:</b> After generation, the CAPTCHA text and timestamp are stored in session:
 * <ul>
 *   <li>{@code KAPTCHA_SESSION_KEY}: The correct CAPTCHA text</li>
 *   <li>{@code KAPTCHA_SESSION_DATE}: Generation timestamp (milliseconds)</li>
 * </ul>
 * These are used by {@link UserServlet} for validation during login.
 * </p>
 * <p>
 * <b>Security Headers:</b> Response includes cache-prevention headers to ensure browsers
 * don't cache CAPTCHA images:
 * <ul>
 *   <li>Expires: 0</li>
 *   <li>Cache-Control: no-store, no-cache, must-revalidate</li>
 *   <li>Pragma: no-cache</li>
 * </ul>
 * </p>
 *
 * @author Jflame
 * @version 4.0.0
 * @since 2026/5/11
 * @see UserServlet
 * @see Constants#KAPTCHA_SESSION_KEY
 */
@WebServlet(name = "CaptchaServlet", urlPatterns = "/captcha")
public class CaptchaServlet extends HttpServlet {

    private Producer captchaProducer;

    /**
     * Initializes the CAPTCHA producer with custom configuration.
     * <p>
     * This method configures the Kaptcha library with specific properties for image generation.
     * Called once when the servlet is first loaded by the container.
     * </p>
     * <p>
     * <b>Configuration Properties:</b>
     * <ul>
     *   <li>kaptcha.image.width/height: Image dimensions</li>
     *   <li>kaptcha.textproducer.char.string: Allowed characters</li>
     *   <li>kaptcha.textproducer.char.length: Number of characters</li>
     *   <li>kaptcha.textproducer.font.size: Font size in points</li>
     *   <li>kaptcha.noise.impl: Noise generator (disabled for cleaner images)</li>
     *   <li>kaptcha.obscurificator.impl: Visual distortion effect</li>
     * </ul>
     * </p>
     *
     * @param config Servlet configuration object
     * @throws ServletException if initialization fails
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        Properties props = new Properties();
        props.setProperty("kaptcha.image.width", "150");
        props.setProperty("kaptcha.image.height", "50");
        props.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        props.setProperty("kaptcha.textproducer.char.length", "4");
        props.setProperty("kaptcha.textproducer.font.size", "40");
        props.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        props.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");

        Config configObj = new Config(props);
        this.captchaProducer = configObj.getProducerImpl();
    }

    /**
     * Generates a CAPTCHA image and sends it to the client.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Sets cache-prevention headers to force fresh image on each request</li>
     *   <li>Sets content type to image/jpeg</li>
     *   <li>Generates random CAPTCHA text using configured character set</li>
     *   <li>Stores CAPTCHA text and timestamp in user session</li>
     *   <li>Creates BufferedImage with CAPTCHA text and visual effects</li>
     *   <li>Writes image to response output stream as JPEG</li>
     * </ol>
     * </p>
     * <p>
     * <b>Session Attributes Set:</b>
     * <ul>
     *   <li>{@code KAPTCHA_SESSION_KEY}: Generated CAPTCHA text (e.g., "A3K9")</li>
     *   <li>{@code KAPTCHA_SESSION_DATE}: Current time in milliseconds</li>
     * </ul>
     * </p>
     * <p>
     * <b>Security:</b> Cache headers prevent browsers from storing CAPTCHA images,
     * ensuring users always see a fresh challenge.
     * </p>
     *
     * @param req  HttpServletRequest from client
     * @param resp HttpServletResponse for sending CAPTCHA image
     * @throws ServletException if servlet error occurs
     * @throws IOException      if I/O error occurs during image writing
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.addHeader("Cache-Control", "post-check=0, pre-check=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setContentType("image/jpeg");

        String capText = captchaProducer.createText();

        req.getSession().setAttribute(Constants.KAPTCHA_SESSION_KEY, capText);
        req.getSession().setAttribute(Constants.KAPTCHA_SESSION_DATE, System.currentTimeMillis());

        BufferedImage bi = captchaProducer.createImage(capText);
        javax.imageio.ImageIO.write(bi, "jpg", resp.getOutputStream());
    }
}

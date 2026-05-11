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
 * @author Jflame
 * @version 4.0.0
 * @since 2026/5/11
 */
@WebServlet(name = "CaptchaServlet", urlPatterns = "/captcha")
public class CaptchaServlet extends HttpServlet {

    private Producer captchaProducer;

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

package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/demo/random")
public class RandomGeneratorServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String LETTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        String type = request.getParameter("type");

        if (type == null || type.trim().isEmpty()) {
            response.setStatus(
                    SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "Missing parameter. Use type=Number, "
                    + "type=Letters, or type=Random.");
            return;
        }

        switch (type.trim()) {

            case "Number":
                response.getWriter().write(generateNumbers());
                break;

            case "Letters":
                response.getWriter().write(generateLetters());
                break;

            case "Random":
                response.getWriter().write(generateMixed());
                break;

            default:
                response.setStatus(
                        SlingHttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                        "Invalid type. Use Number, Letters, or Random.");
        }
    }

    private String generateNumbers() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            result.append(RANDOM.nextInt(10));
        }

        return result.toString();
    }

    private String generateLetters() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            result.append(
                    LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        return result.toString();
    }

    private String generateMixed() {
        List<Character> characters = new ArrayList<>();

        // Generate exactly 3 random digits.
        for (int i = 0; i < 3; i++) {
            characters.add((char) ('0' + RANDOM.nextInt(10)));
        }

        // Generate exactly 3 random uppercase letters.
        for (int i = 0; i < 3; i++) {
            characters.add(
                    LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        // Mix the digits and letters into a random order.
        Collections.shuffle(characters, RANDOM);

        StringBuilder result = new StringBuilder();

        for (Character character : characters) {
            result.append(character);
        }

        return result.toString();
    }
}
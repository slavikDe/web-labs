package com.example.servlets;

import java.io.IOException;
import java.util.Optional;

import com.example.dao.UserDao;
import com.example.model.User;
import com.example.util.JwtUtil;
import com.example.util.PasswordUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebServlet("/users/*")
public class AuthServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String authHeader = req.getHeader("Authorization");

            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Authorization token required\"}");
                return;
            }

            String token = authHeader.replace("Bearer ", "");

            if(!JwtUtil.isTokenValid(token)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            String role = JwtUtil.getRoleFromToken(token);
            String userId = JwtUtil.getUserIdFromToken(token);

            if("admin".equalsIgnoreCase(role)) {
                var users = userDao.findAll();
                users.forEach(user -> user.setPasswordHash(null));
                resp.getWriter().write(objectMapper.writeValueAsString(users));
            } else {
                Optional<User> user = userDao.findById(userId);
                if(user.isPresent()) {
                    user.get().setPasswordHash(null);
                    resp.getWriter().write(objectMapper.writeValueAsString(user.get()));
                }
                else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\": \"User not found\"}");
                }
            }
        } catch (Exception e) {
            log.error("Error in doGet", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if ("/register".equals(pathInfo)) {
            handleRegister(req, resp);
        } else if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonNode json = objectMapper.readTree(req.getReader());

            String username = String.valueOf(json.get("username"));
            String email = json.get("email").asText(null);
            String password = json.get("password").asText(null);

            if (email == null || email.trim().isEmpty()) {
                writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Email is required");
                return;
            }
            if (username == null || username.trim().isEmpty()) {
                username = email;
            }
            if (password == null || password.length() < 6) {
                writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters");
                return;
            }

            if (userDao.existsByEmail(email)) {
                writeJsonResponse(resp, HttpServletResponse.SC_CONFLICT, "Email already exists");
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(password);
            User user = new User(username, email, hashedPassword);
            user = userDao.save(user);

//            String token = JwtUtil.generateToken(user);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(
                    String.valueOf("{\"message\": \"User registered successfully\", \"}")
            );
            log.info("User registered successfully: {}", username);

        } catch (Exception e) {
            log.error("Error during registration", e);
            writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonNode json = objectMapper.readTree(req.getReader());

            String email = json.get("email").asText(null);
            String password = json.get("password").asText(null);

            if (email == null || password == null) {
                writeJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required");
                return;
            }

            // Find user
            Optional<User> userOpt = userDao.findByEmail(email);
            if (userOpt.isEmpty()) {
                writeJsonResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            User user = userOpt.get();

            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                writeJsonResponse(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            // Generate token
            String token = JwtUtil.generateToken(user);

            resp.getWriter().write(
                    String.format("{\"message\": \"Login successful\", \"token\": \"%s\", \"role\": \"%s\"}", token, user.getRole())
            );
            log.info("User logged in successfully: {}", email);

        } catch (Exception e) {
            log.error("Error during login", e);
            writeJsonResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
        }
    }

    private void writeJsonResponse(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

}

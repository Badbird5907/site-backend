package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.badbird.backend.model.User;
import dev.badbird.backend.repositories.RoleRepository;
import dev.badbird.backend.repositories.UserRepository;
import dev.badbird.backend.security.UserDetailsImpl;
import dev.badbird.backend.security.jwt.JwtUtils;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private Gson gson;

    @Autowired
    private JwtUtils jwtUtils;

    @RequestMapping("/test")
    public ResponseEntity<?> test() {
        if (System.getProperty("dev", "false").equals("true")) {
            try {
                String pwd = encoder.encode("123456789");
                User user = new User("Test", pwd);
                user.setRoles(new HashSet<>(roleRepository.findAll()));
                userRepository.save(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok("Test");
    }


    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("Username: " + loginRequest.getUsername() + " | Password: " + loginRequest.getPassword());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid username or password");
        }


        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getUsername(),
                roles
        ));
    }

    @PostMapping("/check")
    public ResponseEntity<?> check(@Valid @RequestBody TokenCheckRequest request) {
        System.out.println("Request: " + request + " | Token: " + request.getToken());
        JsonObject response = new JsonObject();
        response.addProperty("success", jwtUtils.validateJwtToken(request.getToken()));
        if (response.get("success").getAsBoolean()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            response.addProperty("username", userDetails.getUsername());
            JsonArray roles = new JsonArray();
            userDetails.getAuthorities().forEach(role -> roles.add(new JsonPrimitive(role.getAuthority())));
            response.add("roles", roles);
        }
        return ResponseEntity.ok(gson.toJson(response));
    }


    @PostMapping("/changepwd")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> changePwd(@RequestBody @Valid ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword(), newPassword = request.getNewPassword();

        if (oldPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"Old password cannot be empty\"}");
        }
        if (newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"New password cannot be empty\"}");
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"Password is too short (min. 6 characters)\"}");
        }

        if (newPassword.length() > 40) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"Password is too long (max. 40 characters)\"}");
        }

        User user = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new RuntimeException("Error: User not found."));
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"Old password is incorrect\"}");
        }
        String n = encoder.encode(newPassword);
        if (encoder.matches(oldPassword, n)) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\":\"New password cannot be the same as the old one\"}");
        }
        user.setPassword(n);
        userRepository.save(user);
        return ResponseEntity.ok("{\"success\":true}");
    }

    @PostMapping("/logout")
    //@PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return ResponseEntity.ok("{\"success\": true}");
        }
        return ResponseEntity.ok("{\"success\": true}");
    }

    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private String id;
        private String username;
        private List<String> roles;

        public JwtResponse(String token, String username,
                           List<String> roles) {
            this.token = token;
            this.username = username;
            this.roles = roles;
        }
    }

    @Getter
    @Setter
    public class ChangePasswordRequest {
        @NotBlank
        @Size(min = 6, max = 40)
        private String oldPassword, newPassword;
    }

    @Getter
    @Setter
    public static class TokenCheckRequest {
        @NotBlank
        private String token;
    }
}

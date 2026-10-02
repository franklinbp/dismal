package com.dismal.distribuciones.modules.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dismal.distribuciones.modules.admin.dto.AdminUserRequest;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void adminCanCreateAndListUsers() throws Exception {
        AdminUserRequest request = new AdminUserRequest(
                "Ana",
                "Admin",
                "ana.admin@example.com",
                "+16505551234",
                Role.MANAGER,
                CustomerType.FINAL,
                true,
                null,
                null,
                false,
                BigDecimal.ZERO,
                0,
                "Secret123!"
        );

                mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.admin@example.com"))
                .andExpect(jsonPath("$.customerType").value("FINAL"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("type", "INTERNAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void emptyDatabaseReturnsOkList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("type", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void validationReturnsBadRequest() throws Exception {
        AdminUserRequest request = new AdminUserRequest(
                "Carlos",
                "Cliente",
                "carlos@example.com",
                "+16505551234",
                Role.USER,
                CustomerType.FINAL,
                true,
                null,
                null,
                true,
                new BigDecimal("-1"),
                10,
                "Secret123!"
        );

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void managerCannotChangeRoleOrResetPassword() throws Exception {
        User user = createUser(Role.USER, "client@example.com");

        AdminUserRequest request = new AdminUserRequest(
                "Client",
                "One",
                "client@example.com",
                "+16505551234",
                Role.ADMIN,
                CustomerType.FINAL,
                true,
                null,
                null,
                false,
                BigDecimal.ZERO,
                0,
                "Secret123!"
        );

        mockMvc.perform(put("/api/v1/admin/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/users/" + user.getId() + "/reset-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void managerCannotUpdateInternalUsers() throws Exception {
        User adminUser = createUser(Role.ADMIN, "admin@example.com");

        AdminUserRequest request = new AdminUserRequest(
                "Admin",
                "Two",
                "admin@example.com",
                "+16505551234",
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                "Secret123!"
        );

        mockMvc.perform(put("/api/v1/admin/users/" + adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private User createUser(Role role, String email) {
        User user = User.builder()
                .firstname("Test")
                .lastname("User")
                .email(email)
                .password(passwordEncoder.encode("Secret123!"))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }
}

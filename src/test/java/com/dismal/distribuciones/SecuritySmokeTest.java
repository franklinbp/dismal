package com.dismal.distribuciones;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void protectedEndpointRequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/admin/users", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}

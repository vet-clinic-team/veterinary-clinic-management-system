package com.efe.veterinaryclinic.common.exception;

import com.efe.veterinaryclinic.common.dto.ApiErrorResponse;
import com.efe.veterinaryclinic.owner.Owner;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundExceptionMapsTo404() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/owners/99");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Owner not found"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo("Owner not found");
        assertThat(response.getBody().path()).isEqualTo("/api/owners/99");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void conflictExceptionMapsTo409() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/owners/12");

        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Owner has 2 pet(s) and cannot be deleted"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Owner has 2 pet(s) and cannot be deleted");
    }

    @Test
    void propertyReferenceExceptionMapsTo400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/owners");

        ResponseEntity<ApiErrorResponse> response = handler.handlePropertyReference(
                new PropertyReferenceException("string", TypeInformation.of(Owner.class), List.of()), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid sort field: string");
        assertThat(response.getBody().path()).isEqualTo("/api/owners");
    }

    @Test
    void dataIntegrityViolationMapsTo400WithoutLeakingDbMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/owners");
        when(request.getMethod()).thenReturn("POST");

        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: value too long for type character varying(254); column \"email\"");

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid request data");
        assertThat(response.getBody().message())
                .doesNotContain("varying", "PSQLException", "SQL", "column", "email");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }
}

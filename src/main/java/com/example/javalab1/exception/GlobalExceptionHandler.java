package com.example.javalab1.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<Map<String, String>> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.toString(
                                error.getDefaultMessage(),
                                "Invalid value"
                        )
                ))
                .toList();

        String detail = errors.stream()
                .map(error -> "Field '" + error.get("field")
                        + "' " + error.get("message") + ".")
                .collect(Collectors.joining(" "));

        String path = ((ServletWebRequest) request)
                .getRequest()
                .getRequestURI();

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "validation",
                "Validation failed",
                detail,
                path
        );

        problem.setProperty("errors", errors);

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.NOT_FOUND,
                "not-found",
                "Resource not found",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "invalid-request",
                "Invalid request",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(problem);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String errorType,
            String title,
            String detail,
            String path
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problem.setType(URI.create(
                "https://cosmo-cats.market/errors/" + errorType
        ));
        problem.setTitle(title);
        problem.setInstance(URI.create(path));

        return problem;
    }
}
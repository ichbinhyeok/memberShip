package org.example.membership.exception;


import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("[404 Not Found] {} - Path: {}", ex.getMessage(), request.getRequestURI());


        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<String> handlePessimisticLock(PessimisticLockingFailureException ex) {
        log.error("[LOCK] Pessimistic lock timeout occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Lock timeout");
    }

    @ExceptionHandler(MySQLTransactionRollbackException.class)
    public ResponseEntity<String> handleMysqlLock(MySQLTransactionRollbackException ex) {
        log.error("[LOCK] MySQL transaction rollback due to lock: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("MySQL lock conflict");
    }
}

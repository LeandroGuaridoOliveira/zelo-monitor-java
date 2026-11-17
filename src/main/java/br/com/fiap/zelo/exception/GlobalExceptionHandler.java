package br.com.fiap.zelo.exception;

import br.com.fiap.zelo.dto.ErroPadraoDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroPadraoDto> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (mensagem.isBlank()) {
            mensagem = "Erro de validação nos campos da requisição.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroPadraoDto("VALIDACAO_ERRO", mensagem));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroPadraoDto> handleResponseStatus(ResponseStatusException ex) {
        String codigo = "ERRO_" + ex.getStatusCode().value();
        String mensagem = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErroPadraoDto(codigo, mensagem));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroPadraoDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroPadraoDto("ACESSO_NEGADO", "Permissão insuficiente para este recurso."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroPadraoDto> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroPadraoDto("NAO_AUTENTICADO", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroPadraoDto> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroPadraoDto("ERRO_INTERNO", ex.getMessage() != null ? ex.getMessage() : "Erro interno inesperado"));
    }
}

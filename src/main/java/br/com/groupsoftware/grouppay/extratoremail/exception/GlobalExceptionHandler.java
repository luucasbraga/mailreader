package br.com.groupsoftware.grouppay.extratoremail.exception;

import br.com.groupsoftware.grouppay.extratoremail.util.log.StacktraceUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * Classe de tratamento global de exceções que captura e processa exceções específicas da aplicação.
 * Utiliza o {@link ControllerAdvice} do Spring para capturar exceções em todo o contexto do controlador.
 * <p>
 * Esta classe inclui métodos que lidam com exceções comuns e retornam respostas apropriadas para os clientes.
 * </p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MailReaderException.class)
    public ResponseEntity<List<String>> handleException(MailReaderException ex) {
        return new ResponseEntity<>(StacktraceUtil.getStacktraceAsList(ex.fillInStackTrace()), HttpStatus.BAD_REQUEST);
    }
}

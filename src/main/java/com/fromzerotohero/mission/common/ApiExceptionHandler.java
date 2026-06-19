package com.fromzerotohero.mission.common;

import com.fromzerotohero.mission.workitem.WorkItemNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.fromzerotohero.mission.agent.AgentApprovalException;
import com.fromzerotohero.mission.agent.AgentRunNotFoundException;
import com.fromzerotohero.mission.agent.InvalidIdempotencyKeyException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(WorkItemNotFoundException.class)
    ProblemDetail notFound(WorkItemNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Work item not found");
        detail.setType(URI.create("https://from-zero-to-hero.dev/problems/work-item-not-found"));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Request validation failed");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Invalid request");
        detail.setType(URI.create("https://from-zero-to-hero.dev/problems/invalid-request"));
        return detail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflict(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This work item changed while you were editing it. Refresh and try again.");
        detail.setTitle("Concurrent update conflict");
        detail.setType(URI.create("https://from-zero-to-hero.dev/problems/concurrent-update"));
        return detail;
    }

    @ExceptionHandler(AgentRunNotFoundException.class)
    ProblemDetail agentRunNotFound(AgentRunNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Agent run not found");
        return detail;
    }

    @ExceptionHandler(AgentApprovalException.class)
    ProblemDetail approvalConflict(AgentApprovalException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        detail.setTitle("Agent approval conflict");
        return detail;
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ProblemDetail invalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid idempotency key");
        return detail;
    }
}

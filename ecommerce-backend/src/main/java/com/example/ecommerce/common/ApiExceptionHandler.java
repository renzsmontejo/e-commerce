package com.example.ecommerce.common;

import com.example.ecommerce.product.controller.ProductController;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice(assignableTypes={ProductController.class,
        com.example.ecommerce.cart.controller.CartController.class,
        com.example.ecommerce.order.controller.CheckoutController.class,
        com.example.ecommerce.order.controller.OrderController.class,
        com.example.ecommerce.payment.controller.PaymentController.class})
public class ApiExceptionHandler {
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> invalidParameters(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid request parameters"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Request conflicts with existing or related data"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(413).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,"Image upload exceeds the configured size limit"));
    }
}

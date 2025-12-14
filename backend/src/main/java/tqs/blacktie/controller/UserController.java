package tqs.blacktie.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.dto.UserResponse;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.ReviewService;
import tqs.blacktie.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ReviewService reviewService;
    private final UserService userService;

    public UserController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
            .map(user -> new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAddress(),
                user.getBusinessInfo(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
            ))
            .collect(Collectors.toList());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> setUserRole(@PathVariable Long id, @Valid @RequestBody SetRoleRequest request) {
        try {
            User updated = userService.setUserRole(id, request.getRole());
            return ResponseEntity.ok(new UserResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole(),
                updated.getPhone(),
                updated.getAddress(),
                updated.getBusinessInfo(),
                updated.getCreatedAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAddress(),
                user.getBusinessInfo(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
            ));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        try {
            User updated = userService.updateProfile(id, request);
            return ResponseEntity.ok(new UserResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole(),
                updated.getPhone(),
                updated.getAddress(),
                updated.getBusinessInfo(),
                updated.getCreatedAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/reputation")
    public ResponseEntity<UserResponse> getUserReputation(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getUserReputation(id));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        errors.put("message", "Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }


}

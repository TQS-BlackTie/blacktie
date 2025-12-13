package tqs.blacktie.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        // Hash the password using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
            request.getName(),
            request.getEmail(),
            hashedPassword
        );

        return userRepository.save(user);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Check if user is suspended or banned (admin users are always allowed)
        String status = user.getStatus();
        if (status != null && !User.ROLE_ADMIN.equals(user.getRole())) {
            if (User.STATUS_SUSPENDED.equals(status)) {
                throw new IllegalArgumentException("Your account has been suspended. Please contact support.");
            }
            if (User.STATUS_BANNED.equals(status)) {
                throw new IllegalArgumentException("Your account has been banned.");
            }
        }

        return user;
    }

    public User setUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedRole = newRole.toLowerCase().trim();

        if (normalizedRole.equals(User.ROLE_ADMIN)) {
            throw new IllegalArgumentException("Cannot set role to admin");
        }

        if (!normalizedRole.equals(User.ROLE_RENTER) && !normalizedRole.equals(User.ROLE_OWNER)) {
            throw new IllegalArgumentException("Invalid role. Only 'renter' or 'owner' are allowed");
        }

        user.setRole(normalizedRole);
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }

        if (request.getBusinessInfo() != null) {
            user.setBusinessInfo(request.getBusinessInfo().trim());
        }

        return userRepository.save(user);
    }
}

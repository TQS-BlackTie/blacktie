package tqs.blacktie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

@Component
public class AdminDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataLoader.class);
    private static final String ADMIN_EMAIL = "admin@blacktie.pt";
    private static final String OLD_ADMIN_EMAIL = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.password:admin}")
    private String adminPassword;

    public AdminDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if admin user already exists with new email
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            logger.info("Admin user already exists");
            return;
        }

        // Check if old admin exists and update it
        var oldAdmin = userRepository.findByEmail(OLD_ADMIN_EMAIL);
        if (oldAdmin.isPresent()) {
            User admin = oldAdmin.get();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            logger.info("Admin user updated with email: {}", ADMIN_EMAIL);
            return;
        }

        // Create new admin user
        User admin = new User(
            "Platform Admin",
            ADMIN_EMAIL,
            passwordEncoder.encode(adminPassword),
            User.ROLE_ADMIN
        );
        userRepository.save(admin);
        logger.info("Admin user created with email: {}", ADMIN_EMAIL);
    }
}

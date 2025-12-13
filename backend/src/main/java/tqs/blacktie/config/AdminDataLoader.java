package tqs.blacktie.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

@Component
public class AdminDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if admin user already exists with new email
        if (userRepository.findByEmail("admin@blacktie.pt").isPresent()) {
            System.out.println("Admin user already exists");
            return;
        }

        // Check if old admin exists and update it
        var oldAdmin = userRepository.findByEmail("admin");
        if (oldAdmin.isPresent()) {
            User admin = oldAdmin.get();
            admin.setEmail("admin@blacktie.pt");
            admin.setPassword(passwordEncoder.encode("admin"));
            userRepository.save(admin);
            System.out.println("Admin user updated with email: admin@blacktie.pt");
            return;
        }

        // Create new admin user
        User admin = new User(
            "Platform Admin",
            "admin@blacktie.pt",
            passwordEncoder.encode("admin"),
            "admin"
        );
        userRepository.save(admin);
        System.out.println("Admin user created with email: admin@blacktie.pt");
    }
}

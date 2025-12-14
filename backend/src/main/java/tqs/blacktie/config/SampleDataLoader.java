package tqs.blacktie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tqs.blacktie.entity.*;
import tqs.blacktie.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(1)
public class SampleDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SampleDataLoader.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Value("${sample.data.enabled:true}")
    private boolean sampleDataEnabled;

    public SampleDataLoader(UserRepository userRepository,
            ProductRepository productRepository,
            BookingRepository bookingRepository,
            ReviewRepository reviewRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!sampleDataEnabled) {
            logger.info("Sample data loading is disabled");
            return;
        }

        // Check if data already exists
        if (userRepository.count() > 0) {
            logger.info("Database already has data, skipping sample data loading");
            return;
        }

        logger.info("Loading sample data for BlackTie formal events rental platform...");

        // Create Admin
        User admin = createUser("Platform Admin", "admin@blacktie.pt", adminPassword, User.ROLE_ADMIN);

        // Create Owners (formal wear boutiques)
        User owner1 = createUser("Elegância Lisboa", "elegancia@blacktie.pt", "owner123", User.ROLE_OWNER);
        owner1.setPhone("+351 21 555 0101");
        owner1.setAddress("Avenida da Liberdade 245, Lisboa");
        owner1.setBusinessInfo(
                "Premium formal wear rentals since 1985. Specializing in bespoke tuxedos and evening gowns.");
        userRepository.save(owner1);

        User owner2 = createUser("Gala Porto", "gala@blacktie.pt", "owner123", User.ROLE_OWNER);
        owner2.setPhone("+351 22 555 0202");
        owner2.setAddress("Rua de Santa Catarina 112, Porto");
        owner2.setBusinessInfo("Luxury accessories and formal attire for galas, weddings, and corporate events.");
        userRepository.save(owner2);

        User owner3 = createUser("Cerimónia Faro", "cerimonia@blacktie.pt", "owner123", User.ROLE_OWNER);
        owner3.setPhone("+351 28 555 0303");
        owner3.setAddress("Rua Conselheiro Bívar 45, Faro");
        owner3.setBusinessInfo(
                "Southern Portugal's finest formal wear collection for beach weddings and summer galas.");
        userRepository.save(owner3);

        // Create Renters
        User renter1 = createUser("João Silva", "joao@email.pt", "renter123", User.ROLE_RENTER);
        renter1.setPhone("+351 91 000 1111");
        renter1.setAddress("Rua Augusta 45, Lisboa");
        userRepository.save(renter1);

        User renter2 = createUser("Maria Santos", "maria@email.pt", "renter123", User.ROLE_RENTER);
        renter2.setPhone("+351 92 000 2222");
        renter2.setAddress("Avenida dos Aliados 78, Porto");
        userRepository.save(renter2);

        User renter3 = createUser("Pedro Costa", "pedro@email.pt", "renter123", User.ROLE_RENTER);
        renter3.setPhone("+351 93 000 3333");
        renter3.setAddress("Praça do Comércio 12, Lisboa");
        userRepository.save(renter3);

        User renter4 = createUser("Ana Ferreira", "ana@email.pt", "renter123", User.ROLE_RENTER);
        renter4.setPhone("+351 96 000 4444");
        renter4.setAddress("Rua de Cedofeita 234, Porto");
        userRepository.save(renter4);

        logger.info("Created {} users", userRepository.count());

        // Create Products - Owner 1 (Lisboa - Elegância Lisboa)
        Product p1 = createProduct("Classic Black Tuxedo",
                "Timeless single-breasted black tuxedo in premium wool. Includes jacket, trousers, and cummerbund. Perfect for black-tie events.",
                75.0, 150.0, owner1, "Lisboa");

        Product p2 = createProduct("Midnight Blue Dinner Jacket",
                "Modern slim-fit dinner jacket in midnight blue with satin lapels. A sophisticated alternative to traditional black.",
                65.0, 120.0, owner1, "Lisboa");

        Product p3 = createProduct("White Dinner Jacket",
                "Elegant ivory white dinner jacket for summer galas and cruise events. Single-breasted with peak lapels.",
                70.0, 130.0, owner1, "Lisboa");

        Product p4 = createProduct("Burgundy Velvet Blazer",
                "Luxurious burgundy velvet smoking jacket with black satin shawl collar. Statement piece for holiday parties.",
                55.0, 100.0, owner1, "Lisboa");

        Product p5 = createProduct("Classic Wing-Tip Dress Shirt",
                "Crisp white formal shirt with wing-tip collar for bow tie. French cuffs included. Premium cotton.",
                15.0, 30.0, owner1, "Lisboa");

        // Create Products - Owner 2 (Porto - Gala Porto)
        Product p6 = createProduct("Emerald Silk Evening Gown",
                "Stunning floor-length emerald green silk gown with draped back. Perfect for charity galas and formal dinners.",
                95.0, 200.0, owner2, "Porto");

        Product p7 = createProduct("Black Sequin Cocktail Dress",
                "Glamorous knee-length black sequin dress. Perfect for cocktail parties and semi-formal events.",
                60.0, 110.0, owner2, "Porto");

        Product p8 = createProduct("Navy Ball Gown",
                "Breathtaking navy blue tulle ball gown with beaded bodice. Ideal for debutante balls and formal galas.",
                120.0, 250.0, owner2, "Porto");

        Product p9 = createProduct("Silver Satin Clutch",
                "Elegant silver satin evening clutch with crystal clasp. Includes detachable chain strap.",
                20.0, 40.0, owner2, "Porto");

        Product p10 = createProduct("Pearl Drop Earrings",
                "Classic South Sea pearl drop earrings in 18k gold setting. Timeless elegance for any formal occasion.",
                35.0, 70.0, owner2, "Porto");

        Product p11 = createProduct("Diamond Tennis Bracelet",
                "Stunning cubic zirconia tennis bracelet in platinum setting. Sparkles like the real thing.",
                45.0, 90.0, owner2, "Porto");

        // Create Products - Owner 3 (Faro - Cerimónia Faro)
        Product p12 = createProduct("Ivory Linen Summer Suit",
                "Breathable ivory linen suit perfect for beach weddings and summer garden parties. Relaxed Mediterranean elegance.",
                60.0, 120.0, owner3, "Faro");

        Product p13 = createProduct("Champagne Maxi Dress",
                "Flowing champagne-colored maxi dress in silk chiffon. Ideal for beach ceremonies and sunset receptions.",
                70.0, 140.0, owner3, "Faro");

        Product p14 = createProduct("Coral Statement Necklace",
                "Bold coral and gold statement necklace. Perfect accent for summer formal wear.",
                25.0, 50.0, owner3, "Faro");

        Product p15 = createProduct("Tan Leather Dress Shoes",
                "Premium tan leather oxford dress shoes. Perfect match for light summer suits.",
                30.0, 60.0, owner3, "Faro");

        Product p16 = createProduct("Silk Pocket Square Set",
                "Set of 5 premium silk pocket squares in complementary colors. Elevate any suit jacket.",
                18.0, 35.0, owner3, "Faro");

        Product p17 = createProduct("Gold Cufflinks & Tie Bar Set",
                "Matching gold-plated cufflinks and tie bar set with subtle geometric pattern.",
                22.0, 45.0, owner3, "Faro");

        Product p18 = createProduct("Bow Tie Collection",
                "Set of 3 silk bow ties: classic black, burgundy, and navy. Pre-tied with adjustable neck strap.",
                20.0, 40.0, owner3, "Faro");

        logger.info("Created {} products", productRepository.count());

        // Create Past Bookings with different statuses
        LocalDateTime now = LocalDateTime.now();

        // Completed bookings (past)
        Booking b1 = createCompletedBooking(renter1, p1,
                now.minusDays(45), now.minusDays(43), "Lisboa Wedding Reception");

        Booking b2 = createCompletedBooking(renter2, p6,
                now.minusDays(40), now.minusDays(38), "Annual Corporate Gala");

        Booking b3 = createCompletedBooking(renter3, p12,
                now.minusDays(35), now.minusDays(33), "Beach Wedding Algarve");

        Booking b4 = createCompletedBooking(renter1, p9,
                now.minusDays(30), now.minusDays(28), "Charity Auction Evening");

        Booking b5 = createCompletedBooking(renter4, p2,
                now.minusDays(25), now.minusDays(23), "New Year's Eve Gala");

        Booking b6 = createCompletedBooking(renter2, p8,
                now.minusDays(20), now.minusDays(17), "Opera Opening Night");

        Booking b7 = createCompletedBooking(renter3, p5,
                now.minusDays(15), now.minusDays(14), "Business Conference Dinner");

        Booking b8 = createCompletedBooking(renter4, p13,
                now.minusDays(10), now.minusDays(8), "Sunset Beach Party");

        // Recent approved booking (upcoming)
        Booking b9 = createApprovedBooking(renter1, p3,
                now.plusDays(5), now.plusDays(7));

        // Pending approval booking
        Booking b10 = new Booking(renter2, p4, now.plusDays(10), now.plusDays(12),
                calculatePrice(p4, now.plusDays(10), now.plusDays(12)));
        b10.setStatus(Booking.STATUS_PENDING_APPROVAL);
        bookingRepository.save(b10);

        logger.info("Created {} bookings", bookingRepository.count());

        // Create Reviews for completed bookings
        createReview(b1, 5, "Perfect fit and excellent quality. The tuxedo made our wedding photos stunning!",
                "RENTER");
        createReview(b1, 5, "Great renter, returned everything in perfect condition.", "OWNER");

        createReview(b2, 5, "The emerald gown was absolutely breathtaking. Received so many compliments!", "RENTER");
        createReview(b2, 5, "Wonderful client, very careful with the dress.", "OWNER");

        createReview(b3, 4,
                "Beautiful linen suit, perfect for the beach setting. Slightly wrinkled on delivery but still great.",
                "RENTER");
        createReview(b3, 5, "Excellent experience, prompt return.", "OWNER");

        createReview(b4, 5, "The clutch was the perfect accessory for my outfit. Very elegant!", "RENTER");

        createReview(b5, 5, "The midnight blue jacket was a hit at the party. Will definitely rent again!", "RENTER");
        createReview(b5, 4, "Good communication and timely return.", "OWNER");

        createReview(b6, 5, "Felt like a princess in this ball gown. Worth every euro!", "RENTER");
        createReview(b6, 5, "Lovely client, treated the gown with great care.", "OWNER");

        createReview(b7, 4, "Classic shirt, fit perfectly. Simple but effective.", "RENTER");

        createReview(b8, 5, "The champagne dress was perfect for the beach sunset. Gorgeous photos!", "RENTER");
        createReview(b8, 5, "Wonderful renter, returned dress spotless.", "OWNER");

        logger.info("Created {} reviews", reviewRepository.count());
        logger.info("Sample data loading completed successfully!");
    }

    private User createUser(String name, String email, String password, String role) {
        User user = new User(name, email, passwordEncoder.encode(password), role);
        user.setStatus(User.STATUS_ACTIVE);
        return userRepository.save(user);
    }

    private Product createProduct(String name, String description, Double price, Double deposit,
            User owner, String city) {
        Product product = new Product(name, description, price);
        product.setOwner(owner);
        product.setAvailable(true);
        product.setDepositAmount(deposit);
        product.setCity(city);
        return productRepository.save(product);
    }

    private Booking createCompletedBooking(User renter, Product product,
            LocalDateTime start, LocalDateTime end, String note) {
        Double price = calculatePrice(product, start, end);
        Booking booking = new Booking(renter, product, start, end, price);
        booking.setStatus(Booking.STATUS_COMPLETED);
        booking.setDeliveryMethod(Booking.DELIVERY_PICKUP);
        booking.setPickupLocation(product.getOwner().getAddress());
        booking.setApprovedAt(start.minusDays(3));
        booking.setPaidAt(start.minusDays(2));
        return bookingRepository.save(booking);
    }

    private Booking createApprovedBooking(User renter, Product product,
            LocalDateTime start, LocalDateTime end) {
        Double price = calculatePrice(product, start, end);
        Booking booking = new Booking(renter, product, start, end, price);
        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod(Booking.DELIVERY_PICKUP);
        booking.setPickupLocation(product.getOwner().getAddress());
        booking.setApprovedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    private Double calculatePrice(Product product, LocalDateTime start, LocalDateTime end) {
        long days = java.time.Duration.between(start, end).toDays();
        if (days < 1)
            days = 1;
        return product.getPrice() * days;
    }

    private void createReview(Booking booking, int rating, String comment, String reviewType) {
        Review review = new Review(booking, rating, comment, reviewType);
        reviewRepository.save(review);
    }
}

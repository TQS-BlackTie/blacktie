package tqs.blacktie.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SampleDataLoader Tests")
class SampleDataLoaderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SampleDataLoader sampleDataLoader;

    @BeforeEach
    void setUp() {
        sampleDataLoader = new SampleDataLoader(
                userRepository,
                productRepository,
                bookingRepository,
                reviewRepository,
                passwordEncoder);
        ReflectionTestUtils.setField(sampleDataLoader, "adminPassword", "testpassword");
        ReflectionTestUtils.setField(sampleDataLoader, "sampleDataEnabled", true);
    }

    @Test
    @DisplayName("Should not load sample data if database already has data")
    void whenDataExists_thenDoNothing() {
        when(userRepository.count()).thenReturn(5L);

        sampleDataLoader.run();

        verify(userRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not load sample data when disabled")
    void whenSampleDataDisabled_thenDoNothing() {
        ReflectionTestUtils.setField(sampleDataLoader, "sampleDataEnabled", false);

        sampleDataLoader.run();

        verify(userRepository, never()).count();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should load sample data when database is empty")
    void whenDatabaseEmpty_thenLoadSampleData() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        sampleDataLoader.run();

        // Verify users were saved (1 admin + 3 owners + 4 renters = 8 initial saves,
        // then owners get updated with business info = 3 more, renters updated = 4
        // more)
        verify(userRepository, atLeast(8)).save(any(User.class));

        // Verify products were created
        verify(productRepository, atLeast(10)).save(any());

        // Verify bookings were created
        verify(bookingRepository, atLeast(5)).save(any());

        // Verify reviews were created
        verify(reviewRepository, atLeast(5)).save(any());
    }
}

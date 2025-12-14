package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tqs.blacktie.dto.GeoApiResponse;
import tqs.blacktie.dto.LocationDTO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private LocationService locationService;

    @BeforeEach
    void setUp() throws Exception {
        locationService = new LocationService();
        
        // Use reflection to inject the mocked WebClient
        Field webClientField = LocationService.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(locationService, webClient);

        // Setup common mock chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void whenGeocodeWithFullAddress_thenReturnsLocationWithCoordinates() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Rua Augusta 100, Lisboa, 1100-053, Portugal",
                38.7139,
                -9.1394,
                "Lisboa",
                "1100-053"
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("Rua Augusta 100", "Lisboa", "1100-053");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(38.7139);
        assertThat(result.getLongitude()).isEqualTo(-9.1394);
        assertThat(result.getCity()).isEqualTo("Lisboa");
        assertThat(result.getPostalCode()).isEqualTo("1100-053");
        assertThat(result.getAddress()).isEqualTo("Rua Augusta 100, Lisboa, 1100-053, Portugal");
    }

    @Test
    void whenGeocodeWithAddressOnly_thenReturnsLocation() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Rua do Comércio, Portugal",
                40.0,
                -8.0,
                null,
                null
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("Rua do Comércio", null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(40.0);
        assertThat(result.getLongitude()).isEqualTo(-8.0);
        assertThat(result.getAddress()).isEqualTo("Rua do Comércio, Portugal");
    }

    @Test
    void whenGeocodeWithCityAndPostalCode_thenReturnsLocation() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Porto, 4000-001, Portugal",
                41.1579,
                -8.6291,
                "Porto",
                "4000-001"
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress(null, "Porto", "4000-001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(41.1579);
        assertThat(result.getLongitude()).isEqualTo(-8.6291);
        assertThat(result.getCity()).isEqualTo("Porto");
        assertThat(result.getPostalCode()).isEqualTo("4000-001");
    }

    @Test
    void whenGeocodeResponseHasNoCityInComponents_thenUsesProvidedCity() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Some Address, Portugal",
                39.5,
                -8.5,
                null,  // No city in response
                "3000-000"
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("Some Address", "Coimbra", "3000-000");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCity()).isEqualTo("Coimbra");  // Should use provided city
    }

    @Test
    void whenGeocodeResponseHasNoPostalCodeInComponents_thenUsesProvidedPostalCode() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Some Address, Lisboa, Portugal",
                38.7,
                -9.1,
                "Lisboa",
                null  // No postal code in response
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("Some Address", "Lisboa", "1000-000");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPostalCode()).isEqualTo("1000-000");  // Should use provided postal code
    }

    @Test
    void whenGeocodeReturnsNoResults_thenReturnsLocationWithProvidedDataOnly() {
        // Arrange
        GeoApiResponse emptyResponse = new GeoApiResponse();
        emptyResponse.setResults(new ArrayList<>());

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(emptyResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("Unknown Address", "Unknown City", "0000-000");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        assertThat(result.getAddress()).isEqualTo("Unknown Address");
        assertThat(result.getCity()).isEqualTo("Unknown City");
        assertThat(result.getPostalCode()).isEqualTo("0000-000");
    }

    @Test
    void whenGeocodeReturnsNullResponse_thenReturnsLocationWithProvidedData() {
        // Arrange
        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.empty());

        // Act
        LocationDTO result = locationService.geocodeAddress("Test Address", "Test City", "1234-567");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        assertThat(result.getAddress()).isEqualTo("Test Address");
        assertThat(result.getCity()).isEqualTo("Test City");
        assertThat(result.getPostalCode()).isEqualTo("1234-567");
    }

    @Test
    void whenGeocodeThrowsException_thenReturnsLocationWithProvidedData() {
        // Arrange
        when(responseSpec.bodyToMono(GeoApiResponse.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        // Act
        LocationDTO result = locationService.geocodeAddress("Error Address", "Error City", "9999-999");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        assertThat(result.getAddress()).isEqualTo("Error Address");
        assertThat(result.getCity()).isEqualTo("Error City");
        assertThat(result.getPostalCode()).isEqualTo("9999-999");
    }

    @Test
    void whenGeocodeWithBlankStrings_thenBuildsQueryCorrectly() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Portugal",
                39.0,
                -9.0,
                null,
                null
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.geocodeAddress("", "", "");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(39.0);
        assertThat(result.getLongitude()).isEqualTo(-9.0);
    }

    @Test
    void whenReverseGeocodeWithValidCoordinates_thenReturnsLocationWithAddress() {
        // Arrange
        GeoApiResponse mockResponse = createMockGeoApiResponse(
                "Praça do Comércio, Lisboa, 1100-148, Portugal",
                38.7078,
                -9.1364,
                "Lisboa",
                "1100-148"
        );

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO result = locationService.reverseGeocode(38.7078, -9.1364);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(38.7078);
        assertThat(result.getLongitude()).isEqualTo(-9.1364);
        assertThat(result.getAddress()).isEqualTo("Praça do Comércio, Lisboa, 1100-148, Portugal");
        assertThat(result.getCity()).isEqualTo("Lisboa");
        assertThat(result.getPostalCode()).isEqualTo("1100-148");
    }

    @Test
    void whenReverseGeocodeReturnsNoResults_thenReturnsLocationWithCoordinatesOnly() {
        // Arrange
        GeoApiResponse emptyResponse = new GeoApiResponse();
        emptyResponse.setResults(new ArrayList<>());

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(emptyResponse));

        // Act
        LocationDTO result = locationService.reverseGeocode(45.0, 10.0);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(45.0);
        assertThat(result.getLongitude()).isEqualTo(10.0);
        assertThat(result.getAddress()).isNull();
        assertThat(result.getCity()).isNull();
        assertThat(result.getPostalCode()).isNull();
    }

    @Test
    void whenReverseGeocodeReturnsNullResponse_thenReturnsLocationWithCoordinatesOnly() {
        // Arrange
        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.empty());

        // Act
        LocationDTO result = locationService.reverseGeocode(40.0, -8.0);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(40.0);
        assertThat(result.getLongitude()).isEqualTo(-8.0);
        assertThat(result.getAddress()).isNull();
        assertThat(result.getCity()).isNull();
        assertThat(result.getPostalCode()).isNull();
    }

    @Test
    void whenReverseGeocodeThrowsException_thenReturnsLocationWithCoordinatesOnly() {
        // Arrange
        when(responseSpec.bodyToMono(GeoApiResponse.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        // Act
        LocationDTO result = locationService.reverseGeocode(41.0, -8.5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(41.0);
        assertThat(result.getLongitude()).isEqualTo(-8.5);
        assertThat(result.getAddress()).isNull();
        assertThat(result.getCity()).isNull();
        assertThat(result.getPostalCode()).isNull();
    }

    @Test
    void whenGeocodeResponseHasNoGeometry_thenReturnsLocationWithNoCoordinates() {
        // Arrange
        GeoApiResponse mockResponse = new GeoApiResponse();
        GeoApiResponse.Result result = new GeoApiResponse.Result();
        result.setFormattedAddress("Test Address");
        result.setGeometry(null);  // No geometry
        result.setAddressComponents(new ArrayList<>());
        mockResponse.setResults(Arrays.asList(result));

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO location = locationService.geocodeAddress("Test", "Test", "1234-567");

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getLatitude()).isNull();
        assertThat(location.getLongitude()).isNull();
        assertThat(location.getAddress()).isEqualTo("Test Address");
    }

    @Test
    void whenGeocodeResponseHasGeometryButNoLocation_thenReturnsLocationWithNoCoordinates() {
        // Arrange
        GeoApiResponse mockResponse = new GeoApiResponse();
        GeoApiResponse.Result result = new GeoApiResponse.Result();
        result.setFormattedAddress("Test Address");
        GeoApiResponse.Geometry geometry = new GeoApiResponse.Geometry();
        geometry.setLocation(null);  // No location
        result.setGeometry(geometry);
        result.setAddressComponents(new ArrayList<>());
        mockResponse.setResults(Arrays.asList(result));

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO location = locationService.geocodeAddress("Test", "Test", "1234-567");

        // Assert
        assertThat(location).isNotNull();
        assertThat(location.getLatitude()).isNull();
        assertThat(location.getLongitude()).isNull();
    }

    @Test
    void whenReverseGeocodeResponseHasNoAddressComponents_thenReturnsLocationWithoutCityAndPostalCode() {
        // Arrange
        GeoApiResponse mockResponse = new GeoApiResponse();
        GeoApiResponse.Result result = new GeoApiResponse.Result();
        result.setFormattedAddress("Some Address");
        GeoApiResponse.Geometry geometry = new GeoApiResponse.Geometry();
        GeoApiResponse.Location location = new GeoApiResponse.Location();
        location.setLat(38.0);
        location.setLng(-9.0);
        geometry.setLocation(location);
        result.setGeometry(geometry);
        result.setAddressComponents(null);  // No address components
        mockResponse.setResults(Arrays.asList(result));

        when(responseSpec.bodyToMono(GeoApiResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        LocationDTO locationDTO = locationService.reverseGeocode(38.0, -9.0);

        // Assert
        assertThat(locationDTO).isNotNull();
        assertThat(locationDTO.getAddress()).isEqualTo("Some Address");
        assertThat(locationDTO.getCity()).isNull();
        assertThat(locationDTO.getPostalCode()).isNull();
    }

    // Helper method to create mock GeoApiResponse
    private GeoApiResponse createMockGeoApiResponse(String formattedAddress, Double lat, Double lng, 
                                                     String city, String postalCode) {
        GeoApiResponse response = new GeoApiResponse();
        GeoApiResponse.Result result = new GeoApiResponse.Result();
        
        result.setFormattedAddress(formattedAddress);
        
        // Set geometry with location
        GeoApiResponse.Geometry geometry = new GeoApiResponse.Geometry();
        GeoApiResponse.Location location = new GeoApiResponse.Location();
        location.setLat(lat);
        location.setLng(lng);
        geometry.setLocation(location);
        result.setGeometry(geometry);
        
        // Set address components
        List<GeoApiResponse.AddressComponent> components = new ArrayList<>();
        
        if (city != null) {
            GeoApiResponse.AddressComponent cityComponent = new GeoApiResponse.AddressComponent();
            cityComponent.setLongName(city);
            cityComponent.setTypes(Arrays.asList("locality"));
            components.add(cityComponent);
        }
        
        if (postalCode != null) {
            GeoApiResponse.AddressComponent postalComponent = new GeoApiResponse.AddressComponent();
            postalComponent.setLongName(postalCode);
            postalComponent.setTypes(Arrays.asList("postal_code"));
            components.add(postalComponent);
        }
        
        result.setAddressComponents(components);
        response.setResults(Arrays.asList(result));
        
        return response;
    }
}

package tqs.blacktie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tqs.blacktie.dto.GeoApiResponse;
import tqs.blacktie.dto.LocationDTO;

@Service
public class LocationService {
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    private static final String GEO_API_BASE_URL = "https://json.geoapi.pt";
    
    private final WebClient webClient;

    public LocationService() {
        this.webClient = WebClient.builder()
                .baseUrl(GEO_API_BASE_URL)
                .build();
    }

    /**
     * Geocode an address using geoapi.pt
     * @param address Full address or partial address
     * @param city City name (optional)
     * @param postalCode Postal code (optional)
     * @return LocationDTO with coordinates and normalized address
     */
    public LocationDTO geocodeAddress(String address, String city, String postalCode) {
        try {
            // Build query string
            StringBuilder query = new StringBuilder();
            if (address != null && !address.isBlank()) {
                query.append(address);
            }
            if (city != null && !city.isBlank()) {
                if (query.length() > 0) query.append(", ");
                query.append(city);
            }
            if (postalCode != null && !postalCode.isBlank()) {
                if (query.length() > 0) query.append(", ");
                query.append(postalCode);
            }
            query.append(", Portugal");

            logger.info("Geocoding address: {}", query);

            GeoApiResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/gps/{query}")
                            .build(query.toString()))
                    .retrieve()
                    .bodyToMono(GeoApiResponse.class)
                    .block();

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                GeoApiResponse.Result firstResult = response.getResults().get(0);
                LocationDTO location = new LocationDTO();
                
                // Set coordinates
                if (firstResult.getGeometry() != null && firstResult.getGeometry().getLocation() != null) {
                    location.setLatitude(firstResult.getGeometry().getLocation().getLat());
                    location.setLongitude(firstResult.getGeometry().getLocation().getLng());
                }
                
                // Parse address components
                if (firstResult.getAddressComponents() != null) {
                    for (GeoApiResponse.AddressComponent component : firstResult.getAddressComponents()) {
                        if (component.getTypes() != null) {
                            if (component.getTypes().contains("locality")) {
                                location.setCity(component.getLongName());
                            } else if (component.getTypes().contains("postal_code")) {
                                location.setPostalCode(component.getLongName());
                            }
                        }
                    }
                }
                
                // Use provided values if not found in response
                if (location.getCity() == null) {
                    location.setCity(city);
                }
                if (location.getPostalCode() == null) {
                    location.setPostalCode(postalCode);
                }
                
                // Use formatted address or original address
                location.setAddress(firstResult.getFormattedAddress() != null ? 
                        firstResult.getFormattedAddress() : address);
                
                logger.info("Successfully geocoded to: lat={}, lng={}", 
                        location.getLatitude(), location.getLongitude());
                
                return location;
            }

            logger.warn("No results found for address: {}", query);
            // Return location with provided data but no coordinates
            LocationDTO location = new LocationDTO();
            location.setAddress(address);
            location.setCity(city);
            location.setPostalCode(postalCode);
            return location;
            
        } catch (Exception e) {
            logger.error("Error geocoding address: {}", e.getMessage(), e);
            // Return location with provided data but no coordinates
            LocationDTO location = new LocationDTO();
            location.setAddress(address);
            location.setCity(city);
            location.setPostalCode(postalCode);
            return location;
        }
    }

    /**
     * Reverse geocode coordinates to get address
     * @param latitude Latitude
     * @param longitude Longitude
     * @return LocationDTO with address information
     */
    public LocationDTO reverseGeocode(Double latitude, Double longitude) {
        try {
            logger.info("Reverse geocoding coordinates: lat={}, lng={}", latitude, longitude);

            GeoApiResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/gps/{lat},{lng}")
                            .build(latitude, longitude))
                    .retrieve()
                    .bodyToMono(GeoApiResponse.class)
                    .block();

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                GeoApiResponse.Result firstResult = response.getResults().get(0);
                LocationDTO location = new LocationDTO();
                
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                location.setAddress(firstResult.getFormattedAddress());
                
                // Parse address components
                if (firstResult.getAddressComponents() != null) {
                    for (GeoApiResponse.AddressComponent component : firstResult.getAddressComponents()) {
                        if (component.getTypes() != null) {
                            if (component.getTypes().contains("locality")) {
                                location.setCity(component.getLongName());
                            } else if (component.getTypes().contains("postal_code")) {
                                location.setPostalCode(component.getLongName());
                            }
                        }
                    }
                }
                
                logger.info("Successfully reverse geocoded to: {}", location.getAddress());
                return location;
            }

            logger.warn("No results found for coordinates: {}, {}", latitude, longitude);
            LocationDTO location = new LocationDTO();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            return location;
            
        } catch (Exception e) {
            logger.error("Error reverse geocoding coordinates: {}", e.getMessage(), e);
            LocationDTO location = new LocationDTO();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            return location;
        }
    }
}

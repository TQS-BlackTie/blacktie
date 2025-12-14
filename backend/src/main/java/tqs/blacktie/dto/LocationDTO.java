package tqs.blacktie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private String address;
    private String city;
    private String postalCode;
    private Double latitude;
    private Double longitude;
}

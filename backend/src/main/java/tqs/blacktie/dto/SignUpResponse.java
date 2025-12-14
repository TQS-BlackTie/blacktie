package tqs.blacktie.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {

    private Long id;
    private String name;
    private String email;
    private String message;
}

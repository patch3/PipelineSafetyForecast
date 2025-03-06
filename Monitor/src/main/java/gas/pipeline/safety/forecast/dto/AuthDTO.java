package gas.pipeline.safety.forecast.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthDTO {
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя пользователя может содержать только буквы, цифры и символы подчеркивания")
    @Size(min = 3, max = 20, message = "Имя пользователя должно содержать от 3 до 20 символов")
    private String username;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[^\\s]+$", message = "Пароль должен содержать буквы, цифры и специальные символы")
    @Size(min = 8, max = 25, message = "Пароль должен содержать от 8 до 25 символов")
    private String password;

    public AuthDTO() {
        username = "";
        email = "";
        password = "";
    }
}

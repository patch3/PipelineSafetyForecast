package gas.pipeline.safety.forecast.controller;

import gas.pipeline.safety.forecast.dto.AuthDTO;
import gas.pipeline.safety.forecast.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final RegistrationService registrationService;

    @Autowired
    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registred", required = false) String reg,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Ошибка входа. Проверьте имя пользователя и пароль.");
        }
        if (logout != null) {
            model.addAttribute("logout", "Вы успешно вышли из системы.");
        }
        if (reg != null) {
            model.addAttribute("registred", "Вы успешно зарегистрировались");
        }
        return "login";
    }

    @GetMapping("/registration")
    public String registration(
            @RequestParam(value = "success", required = false) String success,
            Model model
    ) {
        model.addAttribute("form", new AuthDTO());
        if (success != null) {
            model.addAttribute("success", "Регистрация прошла успешно!");
        }
        return "registration";
    }

    @PostMapping("/registration")
    public String registerUser(
            @Valid @ModelAttribute("form") AuthDTO dto,
            BindingResult result,
            Model model
    ) {
        model.addAttribute("form", dto);
        if (result.hasErrors()) {
            model.addAttribute("errors", result.getAllErrors());

            return "registration";
        }
        val isRegist = registrationService.registerUser(dto.getUsername(), dto.getEmail(), dto.getPassword());
        if (!isRegist) {
            model.addAttribute("errors", "Пользователь c такими данными уже зарегистрирован");
            model.addAttribute("form", dto);
            return "registration";
        }
        return "redirect:/login?registred";
    }

}
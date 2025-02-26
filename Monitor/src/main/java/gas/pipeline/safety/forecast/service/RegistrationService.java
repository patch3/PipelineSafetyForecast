package gas.pipeline.safety.forecast.service;

import gas.pipeline.safety.forecast.model.Employee;
import gas.pipeline.safety.forecast.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean registerUser(String username, String email, String password) {
        if (employeeRepository.findByUsername(username).isPresent() ||
                employeeRepository.findByEmail(email).isPresent()) {
            return false;
        }

        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(password));

        employeeRepository.save(employee);
        return true;
    }
}
package gas.pipeline.safety.forecast.service;

import gas.pipeline.safety.forecast.model.Employee;
import gas.pipeline.safety.forecast.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeDetailedServiceImpl implements UserDetailsService {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeDetailedServiceImpl(EmployeeRepository repository) {
        this.employeeRepository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found: " + username));

        return User
                .withUsername(employee.getUsername())
                .password(employee.getPassword())
                .build();
    }
}
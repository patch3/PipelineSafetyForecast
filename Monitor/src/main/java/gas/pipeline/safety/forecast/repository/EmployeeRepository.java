package gas.pipeline.safety.forecast.repository;

import gas.pipeline.safety.forecast.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username); // Переименовано

    Optional<Employee> findByEmail(String email); // Добавлено для регистрации
}
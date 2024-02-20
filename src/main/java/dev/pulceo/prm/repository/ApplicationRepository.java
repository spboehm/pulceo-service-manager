package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.application.Application;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ApplicationRepository extends CrudRepository<Application, Long>{
    Optional<Application> findByName(String name);

}

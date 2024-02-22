package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.application.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends CrudRepository<Application, Long>{

    @EntityGraph(value="graph.Application.applicationComponents")
    Optional<Application> findByName(String name);

    @Override
    @EntityGraph(value="graph.Application.applicationComponents")
    Iterable<Application> findAll();

    Optional<Application> findByUuid(UUID uuid);
}

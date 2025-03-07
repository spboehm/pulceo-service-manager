package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.application.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends CrudRepository<Application, Long>{

    @EntityGraph(value="graph.Application.applicationComponents")
    Optional<Application> findByName(String name);

    @EntityGraph(value="graph.Application.applicationComponents")
    Optional<Application> findByNameAndNodeId(String name, String nodeId);

    @Override
    @EntityGraph(value="graph.Application.applicationComponents")
    Iterable<Application> findAll();

    @EntityGraph(value="graph.Application.applicationComponents")
    Optional<Application> findByUuid(UUID uuid);
}

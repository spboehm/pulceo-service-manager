package dev.pulceo.prm.repository;


import dev.pulceo.prm.model.orchestration.Orchestration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrchestrationRepository extends CrudRepository<Orchestration, Long> {

    Optional<Orchestration> findByName(String name);

    void deleteOrchestrationByName(String expectedOrchestrationName);
    
}

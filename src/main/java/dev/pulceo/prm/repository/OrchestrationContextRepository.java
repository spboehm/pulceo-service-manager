package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.orchestration.OrchestrationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrchestrationContextRepository extends CrudRepository<OrchestrationContext, Long> {
}

package dev.pulceo.prm.repository;

import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import dev.pulceo.prm.model.application.ApplicationComponent;
import dev.pulceo.prm.model.application.ApplicationComponentType;
import org.springframework.data.repository.CrudRepository;

import javax.sql.rowset.CachedRowSet;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationComponentRepository extends CrudRepository<ApplicationComponent, Long> {
    Optional<ApplicationComponent> findByName(String name);

    Optional<ApplicationComponent> findByPort(int port);

    Optional<ApplicationComponent> findByPortAndApplicationComponentType(int port, ApplicationComponentType applicationComponentType);

    Optional<ApplicationComponent> findByNodeUUIDAndPortAndApplicationComponentType(UUID nodeUUID, int port, ApplicationComponentType applicationComponentType);
}

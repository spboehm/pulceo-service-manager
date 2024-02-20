package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.application.ApplicationComponent;
import org.springframework.data.repository.CrudRepository;

import javax.sql.rowset.CachedRowSet;

public interface ApplicationComponentRepository extends CrudRepository<ApplicationComponent, Long> {
}

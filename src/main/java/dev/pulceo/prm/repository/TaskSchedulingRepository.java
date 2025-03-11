package dev.pulceo.prm.repository;


import dev.pulceo.prm.model.task.TaskScheduling;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskSchedulingRepository extends CrudRepository<TaskScheduling, Long> {

    Optional<TaskScheduling> findByUuid(UUID uuid);

}

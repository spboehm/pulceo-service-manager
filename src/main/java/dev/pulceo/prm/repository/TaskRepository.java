package dev.pulceo.prm.repository;


import dev.pulceo.prm.model.task.Task;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends CrudRepository<Task, Long> {

    //@Cacheable(value = "task", key = "#uuid")
    Optional<Task> findByUuid(UUID uuid);

    Optional<Task> findByRemoteTaskUUID(String remoteTaskUUID);

}

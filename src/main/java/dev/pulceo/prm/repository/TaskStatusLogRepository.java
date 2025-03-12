package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.task.TaskStatusLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStatusLogRepository extends CrudRepository<TaskStatusLog, Long> {

}

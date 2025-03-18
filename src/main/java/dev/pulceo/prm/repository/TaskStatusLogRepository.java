package dev.pulceo.prm.repository;

import dev.pulceo.prm.model.task.TaskStatusLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskStatusLogRepository extends CrudRepository<TaskStatusLog, Long> {

    List<TaskStatusLog> findTaskStatusLogsByTaskId(Long id);
}

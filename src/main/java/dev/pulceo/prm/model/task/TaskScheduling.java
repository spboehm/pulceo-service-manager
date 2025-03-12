package dev.pulceo.prm.model.task;

import dev.pulceo.prm.dto.task.TaskSchedulingDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class TaskScheduling extends BaseEntity {

    @Builder.Default
    private String nodeId = ""; // global node id where the task is scheduled
    @Builder.Default
    private String applicationId = ""; // global application id
    @Builder.Default
    private String applicationComponentId = ""; // global application component id
    @Builder.Default
    private TaskStatus status = TaskStatus.NEW; // task status
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TaskStatusLog> statusLogs; // task status logs

    public TaskScheduling addTaskStatusLog(TaskStatusLog taskStatusLog) {
        statusLogs.add(taskStatusLog);
        taskStatusLog.setTaskScheduling(this);
        return this;
    }

    public static TaskScheduling fromTaskSchedulingDTO(@Valid TaskSchedulingDTO taskSchedulingDTO) {
        return TaskScheduling.builder()
                .nodeId(taskSchedulingDTO.getNodeId())
                .applicationId(taskSchedulingDTO.getApplicationId())
                .applicationComponentId(taskSchedulingDTO.getApplicationComponentId())
                .status(taskSchedulingDTO.getStatus())
                .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TaskScheduling that = (TaskScheduling) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "nodeId = " + nodeId + ", " +
                "applicationId = " + applicationId + ", " +
                "applicationComponentId = " + applicationComponentId + ", " +
                "status = " + status + ")";
    }
}

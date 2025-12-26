package dev.pulceo.prm.model.task;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dev.pulceo.prm.dto.task.TaskSchedulingDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@NamedEntityGraph(
        name = "graph.TaskScheduling.statusLogs",
        attributeNodes = {
                @NamedAttributeNode(value = "properties"),
                @NamedAttributeNode(value = "statusLogs")
        })
@NamedEntityGraph(
        name = "graph.TaskScheduling.task.statusLogs",
        attributeNodes = {
                @NamedAttributeNode(value = "properties"),
                @NamedAttributeNode(value = "statusLogs"),
                @NamedAttributeNode(value = "task", subgraph = "task.metaData"),
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "task.metaData",
                        attributeNodes = {
                                @NamedAttributeNode(value = "taskMetaData"),
                                @NamedAttributeNode(value = "properties")
                        })
        }
)
public class TaskScheduling extends BaseEntity {

    @Builder.Default
    private String globalTaskUUID = ""; // gloval UUID on device
    @Builder.Default
    private String remoteTaskUUID = ""; // remote task uuid on deviss
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "task_scheduling_property_key")
    @Column(name = "task_scheduling_property_value")
    @CollectionTable(name = "task_scheduling_properties", joinColumns = @JoinColumn(name = "task_scheduling_property_id"))
    private Map<String, String> properties = new HashMap<>(); // properties of the task
    @Builder.Default
    private String nodeId = ""; // global node id where the task is scheduled
    @NotNull(message = "Remote node id is required!")
    @Builder.Default
    private String remoteNodeUUID = "";
    @NotNull(message = "PNA id is required!")
    @Builder.Default
    private String applicationId = ""; // global application id
    @Builder.Default
    private String applicationComponentId = ""; // global application component id
    @Builder.Default
    private TaskStatus status = TaskStatus.NONE; // task status
    @OneToOne(targetEntity = Task.class, cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "task_id")
    @JsonBackReference
    private Task task; // task
    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.REMOVE}, mappedBy = "taskScheduling")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<TaskStatusLog> statusLogs = new ArrayList<>(); // task status logs

    public void addTask(Task task) {
        this.task = task;
        task.setTaskScheduling(this);
    }

    public void addTaskStatusLog(TaskStatusLog taskStatusLog) {
        statusLogs.add(taskStatusLog);
        taskStatusLog.setTaskScheduling(this);
    }

    public static TaskScheduling fromTaskSchedulingDTO(@Valid TaskSchedulingDTO taskSchedulingDTO) {
        return TaskScheduling.builder()
                .nodeId(taskSchedulingDTO.getNodeId())
                .applicationId(taskSchedulingDTO.getApplicationId())
                .applicationComponentId(taskSchedulingDTO.getApplicationComponentId())
                .status(taskSchedulingDTO.getStatus())
                .properties(taskSchedulingDTO.getProperties())
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

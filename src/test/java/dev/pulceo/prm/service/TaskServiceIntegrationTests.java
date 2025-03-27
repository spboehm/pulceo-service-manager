package dev.pulceo.prm.service;

import dev.pulceo.prm.model.message.TaskStatusLogMessage;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.PublishSubscribeChannel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"webclient.scheme=http"})
public class TaskServiceIntegrationTests {


    @Autowired
    private TaskService taskService;

    @Autowired
    private PublishSubscribeChannel taskServiceChannel;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusLogRepository taskStatusLogRepository;

    @BeforeEach
    public void setup() {
        this.taskStatusLogRepository.deleteAll();
        this.taskRepository.deleteAll();
    }

    @Test
    public void testCreateTask() throws InterruptedException {
        // given
        Task task = Task.builder()
                .sizeOfWorkload(10000)
                .payload(new byte[10000])
                .build();

        // when
        BlockingQueue<TaskStatusLogMessage> taskStatusLogMessageBlockingQueue = new ArrayBlockingQueue<>(10);
        this.taskServiceChannel.subscribe(message -> taskStatusLogMessageBlockingQueue.add((TaskStatusLogMessage) message.getPayload()));
        // create task and wait for result
        this.taskService.createTask(task);
        TaskStatusLogMessage taskStatusLogMessage = taskStatusLogMessageBlockingQueue.take();

        // then
        assertNotNull(taskStatusLogMessage);
        assertNotNull(taskStatusLogMessage.getTaskUUID());
        assertNotNull(taskStatusLogMessage.getTimestamp());
        assertEquals("NONE", taskStatusLogMessage.getPreviousStatus());
        assertEquals("NEW", taskStatusLogMessage.getNewStatus());
        assertNotNull(taskStatusLogMessage.getModifiedOn());
        assertEquals("psm", taskStatusLogMessage.getModifiedBy());
        assertNotNull(taskStatusLogMessage.getModifiedById());
        assertEquals("TaskScheduling(nodeId = , applicationId = , applicationComponentId = , status = NONE)", taskStatusLogMessage.getPreviousStateOfTask());
        assertEquals("TaskScheduling(nodeId = , applicationId = , applicationComponentId = , status = NEW)", taskStatusLogMessage.getNewStateOfTask());
        assertNotNull(taskStatusLogMessage.getTaskSchedulingUUID());
        assertEquals("", taskStatusLogMessage.getComment());
    }

}

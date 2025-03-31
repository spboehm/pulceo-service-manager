package dev.pulceo.prm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.pulceo.prm.api.exception.PnaApiException;
import dev.pulceo.prm.dto.message.Operation;
import dev.pulceo.prm.dto.message.ResourceMessage;
import dev.pulceo.prm.dto.message.ResourceType;
import dev.pulceo.prm.dto.task.UpdateTaskFromPNADTO;
import dev.pulceo.prm.exception.TaskServiceException;
import dev.pulceo.prm.model.message.TaskStatusLogMessage;
import dev.pulceo.prm.model.task.Task;
import dev.pulceo.prm.model.task.TaskScheduling;
import dev.pulceo.prm.model.task.TaskStatus;
import dev.pulceo.prm.model.task.TaskStatusLog;
import dev.pulceo.prm.repository.TaskRepository;
import dev.pulceo.prm.repository.TaskSchedulingRepository;
import dev.pulceo.prm.repository.TaskStatusLogRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"webclient.scheme=http"})
public class TaskServiceIntegrationTests {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PublishSubscribeChannel taskServiceChannel;

    @Autowired
    private BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusLogRepository taskStatusLogRepository;

    @Autowired
    private TaskSchedulingRepository taskSchedulingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public static WireMockServer wireMockServerForPRM = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7878));
    public static WireMockServer wireMockServerForPNA = new WireMockServer(WireMockSpring.options().bindAddress("127.0.0.1").port(7676));

    @BeforeAll
    static void setupClass() throws InterruptedException {
        Thread.sleep(1000);
        TaskServiceIntegrationTests.wireMockServerForPRM.start();
        TaskServiceIntegrationTests.wireMockServerForPNA.start();

    }

    @BeforeEach
    public void setup() throws InterruptedException {
        Thread.sleep(1000);
        this.taskRepository.deleteAll();
        this.taskSchedulingRepository.deleteAll();
        this.taskStatusLogRepository.deleteAll();
    }

    @AfterAll
    public static void tearDown() {
        TaskServiceIntegrationTests.wireMockServerForPRM.shutdown();
        TaskServiceIntegrationTests.wireMockServerForPNA.shutdown();
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


    @Test
    public void testUpdateTaskWithNoStatusChange() {
        // given

        // when

        // then
    }

    @Test
    public void testUpdateTaskFromNewToScheduledWithFullLifecycle() throws InterruptedException, TaskServiceException, PnaApiException, JsonProcessingException {
        /* given */
        String nodeID = "0b1c6697-cb29-4377-bcf8-9fd61ac6c0f3";
        String pnaToken = "b0hRUGwxT0hNYnhGbGoyQ2tlQnBGblAxOmdHUHM3MGtRRWNsZVFMSmdZclFhVUExb0VpNktGZ296";
        // (1) create task with status NONE -> NEW
        Task newTask = Task.builder()
                .sizeOfWorkload(1000)
                .payload(new byte[1000])
                .build();

        Task createdTask = this.taskService.createTask(newTask);

        /* when */
        // mock response from PNA
        TaskServiceIntegrationTests.wireMockServerForPNA.stubFor(WireMock.post(urlEqualTo("/api/v1/tasks"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("task/pna-create-task-response.json")));

        // mock response from PRM
        TaskServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/" + nodeID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("api/prmapi-get-node-dto-by-id.json")));

        // mock response from PRM
        TaskServiceIntegrationTests.wireMockServerForPRM.stubFor(WireMock.get(urlEqualTo("/api/v1/nodes/" + nodeID + "/pna-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pnaToken)));

        // (2) update task from NEW to SCHEDULED
        TaskScheduling taskSchedulingRequest = TaskScheduling.builder()
                .nodeId(nodeID)
                .applicationId("")
                .applicationComponentId("")
                .status(TaskStatus.SCHEDULED)
                .build();

        /* then */
        // SCHEDULE the task
        TaskScheduling updatedTaskScheduling = this.taskService.updateTaskScheduling(newTask.getUuid(), taskSchedulingRequest);
        assertEquals(TaskStatus.SCHEDULED, updatedTaskScheduling.getStatus());

        // assert OFFLOADED
        TaskStatus waitingForStatusOffloaded = TaskStatus.OFFLOADED;
        Optional<TaskScheduling> refreshedTaskSchedulingOffloaded = waitForTaskTaskSchedulingUpdate(createdTask, waitingForStatusOffloaded);
        if (refreshedTaskSchedulingOffloaded.isPresent()) {
            TaskScheduling refreshedTaskScheduling = refreshedTaskSchedulingOffloaded.get();
            assertEquals(TaskStatus.OFFLOADED, refreshedTaskScheduling.getStatus());
        } else {
            throw new RuntimeException("Failed to get task scheduling with desired status %s".formatted(waitingForStatusOffloaded));
        }

        // mock status RUNNING update from pna
        TaskStatus runningTaskFromPNA = TaskStatus.RUNNING;
        mockUpdateTaskFromPNA(createdTask, runningTaskFromPNA);

        // assert RUNNING
        TaskStatus waitingForStatusRunning = TaskStatus.RUNNING;
        Optional<TaskScheduling> refreshedTaskSchedulingRunning = waitForTaskTaskSchedulingUpdate(createdTask, waitingForStatusRunning);
        if (refreshedTaskSchedulingRunning.isPresent()) {
            TaskScheduling refreshedTaskScheduling = refreshedTaskSchedulingRunning.get();
            assertEquals(TaskStatus.RUNNING, refreshedTaskScheduling.getStatus());
        } else {
            throw new RuntimeException("Failed to get task scheduling with desired status %s".formatted(waitingForStatusRunning));
        }

        // mock status COMPLETED update from pna
        TaskStatus completedTaskFromPNA = TaskStatus.COMPLETED;
        mockUpdateTaskFromPNA(createdTask, completedTaskFromPNA);

        // assert COMPLETED
        TaskStatus waitingForTaskStatusCompleted = TaskStatus.COMPLETED;
        Optional<TaskScheduling> refreshedTaskSchedulingCompleted = waitForTaskTaskSchedulingUpdate(createdTask, waitingForTaskStatusCompleted);
        if (refreshedTaskSchedulingCompleted.isPresent()) {
            TaskScheduling refreshedTaskScheduling = refreshedTaskSchedulingCompleted.get();
            assertEquals(TaskStatus.COMPLETED, refreshedTaskScheduling.getStatus());
        } else {
            throw new RuntimeException("Failed to get task scheduling with desired status %s".formatted(waitingForTaskStatusCompleted));
        }

        // assert task logs
        List<TaskStatusLog> taskStatusLogs = this.taskService.readAllTaskStatusLogsByTaskId(createdTask.getId());
        assert (taskStatusLogs.size() == 5);
        assert (taskStatusLogs.get(0).getPreviousStatus() == TaskStatus.NONE);
        assert (taskStatusLogs.get(0).getNewStatus() == TaskStatus.NEW);
        assert (taskStatusLogs.get(1).getPreviousStatus() == TaskStatus.NEW);
        assert (taskStatusLogs.get(1).getNewStatus() == TaskStatus.SCHEDULED);
        assert (taskStatusLogs.get(2).getPreviousStatus() == TaskStatus.SCHEDULED);
        assert (taskStatusLogs.get(2).getNewStatus() == TaskStatus.OFFLOADED);
        assert (taskStatusLogs.get(3).getPreviousStatus() == TaskStatus.OFFLOADED);
        assert (taskStatusLogs.get(3).getNewStatus() == TaskStatus.RUNNING);
        assert (taskStatusLogs.get(4).getPreviousStatus() == TaskStatus.RUNNING);
        assert (taskStatusLogs.get(4).getNewStatus() == TaskStatus.COMPLETED);
    }

    // TODO: replace spin wait with reactive approach (event to pms service)
    private Optional<TaskScheduling> waitForTaskTaskSchedulingUpdate(Task createdTask, TaskStatus waitedForTaskStatus) throws TaskServiceException, InterruptedException {
        int retries = 5;
        Optional<TaskScheduling> refreshedTaskSchedulingFromTaskSvc = Optional.empty();
        do {
            refreshedTaskSchedulingFromTaskSvc = this.taskService.readTaskSchedulingByTaskId(createdTask.getUuid().toString());
            if (refreshedTaskSchedulingFromTaskSvc.isPresent()) {
                TaskScheduling refreshedTaskScheduling = refreshedTaskSchedulingFromTaskSvc.get();
                if (refreshedTaskScheduling.getStatus() == waitedForTaskStatus) {
                    break;
                }
            } else {
                throw new TaskServiceException("Task %s not found".formatted(createdTask.getUuid().toString()));
            }
            Thread.sleep(2000);
        } while (refreshedTaskSchedulingFromTaskSvc.get().getStatus() != waitedForTaskStatus && retries-- > 0);
        return refreshedTaskSchedulingFromTaskSvc;
    }

    private void mockUpdateTaskFromPNA(Task createdTask, TaskStatus taskStatus) throws JsonProcessingException {
        UpdateTaskFromPNADTO updatedToRunningTaskFromPNADTO = UpdateTaskFromPNADTO.builder()
                .globalTaskUUID(createdTask.getUuid().toString())
                .remoteTaskUUID(createdTask.getRemoteTaskUUID())
                .newTaskStatus(taskStatus)
                .modifiedOn(Timestamp.valueOf(LocalDateTime.now()))
                .modifiedByRemoteNodeUUID("8f08e447-7ccd-4657-a873-a1d43a733b1a")
                .build();

        ResourceMessage updatedToRunningResourceMessage = ResourceMessage.builder()
                .resourceType(ResourceType.TASK)
                .sentBydeviceId("0247fea1-3ca3-401b-8fa2-b6f83a469680")
                .operation(Operation.UPDATE)
                .payload(this.objectMapper.writeValueAsString(updatedToRunningTaskFromPNADTO))
                .build();
        this.mqttBlockingQueueTasksFromPna.add(new GenericMessage<>(this.objectMapper.writeValueAsString(updatedToRunningResourceMessage)));
    }


}

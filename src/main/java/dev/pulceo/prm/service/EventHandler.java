package dev.pulceo.prm.service;

import dev.pulceo.prm.model.event.EventType;
import dev.pulceo.prm.model.event.PulceoEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EventHandler {

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final BlockingQueue<PulceoEvent> eventQueue;

    private final PublishSubscribeChannel eventServiceChannel;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);


    @Autowired
    public EventHandler(ThreadPoolTaskExecutor threadPoolTaskExecutor, BlockingQueue<PulceoEvent> eventQueue, PublishSubscribeChannel eventServiceChannel ) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.eventQueue = eventQueue;
        this.eventServiceChannel = eventServiceChannel;
    }

    public void handleEvent(PulceoEvent event) throws InterruptedException {
        this.eventQueue.put(event);
    }

    @PreDestroy
    private void stop() throws InterruptedException {
        isRunning.set(false);
        this.eventQueue.put(PulceoEvent.builder().eventType(EventType.SHUTDOWN).build());
        this.threadPoolTaskExecutor.shutdown();
    }

    @PostConstruct
    public void init() {
        threadPoolTaskExecutor.execute(() -> {
            while (isRunning.get()) {
                try {
                    PulceoEvent event = eventQueue.take();
                    if (event.getEventType() == EventType.SHUTDOWN) {
                        return;
                    }
                    this.eventServiceChannel.send(new GenericMessage<>(event));
                } catch (InterruptedException e) {
                    this.isRunning.set(false);
                    System.out.println("Event handler interrupted");
                    for (PulceoEvent event : eventQueue) {
                        this.eventServiceChannel.send(new GenericMessage<>(event));
                    }
                }
            }
        });
    }


}

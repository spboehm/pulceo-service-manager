package dev.pulceo.prm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class TaskStatisticsService {

    private final AtomicLong throughputCounter = new AtomicLong(0);
    private final LongAdder taskCounter = new LongAdder();
    private final Logger logger = LoggerFactory.getLogger(TaskStatisticsService.class);

    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    public TaskStatisticsService(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    public long incrementTaskNumberAndGet() {
        throughputCounter.incrementAndGet();
        synchronized (taskCounter) {
            taskCounter.increment();
            return taskCounter.longValue();
        }
    }

    public long getNumberOfTasks() {
        return taskCounter.sum();
    }

    //@PostConstruct
    public void init() {
        this.threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            this.logger.info("Requests per second (task arrival rate): " + throughputCounter.get());
            this.logger.info("Total number of tasks: " + getNumberOfTasks());
            // TODO: logg persistently to pms
            throughputCounter.set(0); // Reset the counter
        }, Duration.ofSeconds(1));
    }

}

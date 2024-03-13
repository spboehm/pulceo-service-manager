package dev.pulceo.prm.config;

import dev.pulceo.prm.model.event.PulceoEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@EnableAsync
public class ThreadPoolTaskExecutorConfig {

    @Bean
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        return executor;
    }

    @Bean
    public BlockingQueue<PulceoEvent> eventQueue() {
        return new LinkedBlockingQueue<>();
    }

}

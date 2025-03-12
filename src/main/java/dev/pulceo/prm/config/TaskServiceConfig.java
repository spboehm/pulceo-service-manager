package dev.pulceo.prm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.router.PayloadTypeRouter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TaskServiceConfig {

    @Bean
    public PublishSubscribeChannel taskServiceChannel() {
        return new PublishSubscribeChannel();
    }

    @Autowired
    MessageChannel mqttOutboundTaskChannel;

    public PayloadTypeRouter taskServiceRouter() {
        PayloadTypeRouter router = new PayloadTypeRouter();
        router.setDefaultOutputChannel(mqttOutboundTaskChannel);
        return router;
    }

    @Bean
    public IntegrationFlow routerFlow3() {
        return IntegrationFlow.from("taskServiceChannel")
                .transform(Transformers.toJson())
                .route(taskServiceRouter())
                .get();
    }
}

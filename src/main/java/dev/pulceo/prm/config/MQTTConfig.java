package dev.pulceo.prm.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class MQTTConfig {

    @Value("${pna.mqtt.broker.url}")
    private String mqttBrokerURL;
    @Value("${pna.mqtt.client.username}")
    private String mqttBrokerUsername;
    @Value("${pna.mqtt.client.password}")
    private String mqttBrokerPassword;

    /* Outbound */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{mqttBrokerURL});
        options.setUserName(mqttBrokerUsername);
        options.setPassword(mqttBrokerPassword.toCharArray());
        options.setAutomaticReconnect(true);
        options.setSSLProperties(new Properties());
        factory.setConnectionOptions(options);
        return factory;
    }

    /* events */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(UUID.randomUUID().toString(), mqttClientFactory());
        messageHandler.setAsync(true);
        // internal communication
        messageHandler.setDefaultTopic("dt/pulceo/events");
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        return messageHandler;
    }

    /* tasks */
    @Bean
    public MessageChannel mqttOutboundTaskChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundTaskChannel")
    public MessageHandler mqttOutboundTask() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(UUID.randomUUID().toString(), mqttClientFactory());
        messageHandler.setAsync(true);
        // sunk endpoint, there is no need to set a topic
        messageHandler.setDefaultTopic("tasks/");
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        return messageHandler;
    }

    /* inbound from pna */

    @Bean
    public BlockingQueue<Message<?>> mqttBlockingQueueTasksFromPna() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public MessageChannel mqttInputChannelTasksFromPna() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inboundTasksFromPna() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(UUID.randomUUID().toString(), mqttClientFactory(),
                        "cmd/+/tasks");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.connectComplete(true);
        adapter.setOutputChannel(mqttInputChannelTasksFromPna());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannelTasksFromPna")
    public MessageHandler handler() {
        return message -> {
            try {
                mqttBlockingQueueTasksFromPna().put(message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }
}

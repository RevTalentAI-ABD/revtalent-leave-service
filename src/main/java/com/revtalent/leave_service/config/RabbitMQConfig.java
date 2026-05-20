package com.revtalent.leave_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LEAVE_EXCHANGE = "leave.exchange";
    public static final String LEAVE_APPLIED_QUEUE = "leave.applied.queue";
    public static final String LEAVE_APPLIED_ROUTING_KEY = "leave.applied";
    public static final String LEAVE_STATUS_QUEUE = "leave.status.queue";
    public static final String LEAVE_STATUS_ROUTING_KEY = "leave.status.updated";

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(LEAVE_EXCHANGE);
    }

    @Bean
    public Queue leaveAppliedQueue() {
        return QueueBuilder.durable(LEAVE_APPLIED_QUEUE).build();
    }

    @Bean
    public Queue leaveStatusQueue() {
        return QueueBuilder.durable(LEAVE_STATUS_QUEUE).build();
    }

    @Bean
    public Binding leaveAppliedBinding() {
        return BindingBuilder
                .bind(leaveAppliedQueue())
                .to(leaveExchange())
                .with(LEAVE_APPLIED_ROUTING_KEY);
    }

    @Bean
    public Binding leaveStatusBinding() {
        return BindingBuilder
                .bind(leaveStatusQueue())
                .to(leaveExchange())
                .with(LEAVE_STATUS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
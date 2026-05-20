package com.revtalent.leave_service.service;

import com.revtalent.leave_service.config.RabbitMQConfig;
import com.revtalent.leave_service.dto.LeaveAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishLeaveApplied(LeaveAppliedEvent event) {
        log.info("Publishing leave applied event for employee: {}", event.getEmployeeId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEAVE_EXCHANGE,
                RabbitMQConfig.LEAVE_APPLIED_ROUTING_KEY,
                event
        );
    }

    public void publishLeaveStatusUpdated(LeaveAppliedEvent event) {
        log.info("Publishing leave status updated event: {}", event.getStatus());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEAVE_EXCHANGE,
                RabbitMQConfig.LEAVE_STATUS_ROUTING_KEY,
                event
        );
    }
}
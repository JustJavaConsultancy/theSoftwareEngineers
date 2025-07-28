package tech.justjava.process_manager.config;

import jakarta.annotation.PostConstruct;
import org.flowable.common.engine.impl.event.FlowableEventDispatcherImpl;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.process.listener.JustJavaFlowableListener;

@Configuration
public class FlowableConfig {
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;
    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    RuntimeService runtimeService;
    @Autowired
    AuthenticationManager authenticationManager;

    @PostConstruct
    public void registerEventListener() {
        if (processEngineConfiguration.getEventDispatcher() == null) {
            processEngineConfiguration.setEventDispatcher(new FlowableEventDispatcherImpl());
        }
        processEngineConfiguration.getEventDispatcher().addEventListener(new JustJavaFlowableListener(messagingTemplate, authenticationManager, runtimeService));
    }
}

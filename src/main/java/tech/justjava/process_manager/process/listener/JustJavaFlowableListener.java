package tech.justjava.process_manager.process.listener;


import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tech.justjava.process_manager.account.AuthenticationManager;

import java.util.Map;

@Component
public class JustJavaFlowableListener implements FlowableEventListener {


    private final SimpMessagingTemplate messagingTemplate;
    private final AuthenticationManager authenticationManager;
    private final RuntimeService runtimeService;
    public JustJavaFlowableListener(SimpMessagingTemplate messagingTemplate, AuthenticationManager authenticationManager, RuntimeService runtimeService) {
        this.messagingTemplate = messagingTemplate;
        this.authenticationManager = authenticationManager;
        this.runtimeService = runtimeService;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent) {
            FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;


            // Ensure that the entity is a Task


            if (entityEvent.getEntity() instanceof Task) {
                Task task = (Task) entityEvent.getEntity();

                if (event.getType() == FlowableEngineEventType.TASK_CREATED) {

                    System.out.println(" The Task Name Around Here===="+task.getName());
                    runtimeService.setVariable(task.getProcessInstanceId(), "currentTask", task.getName());
                    String setAssignee= (String) runtimeService.getVariable(task.getProcessInstanceId(),"initiator");
                    Map<String,Object> processVariables=runtimeService.getVariables(task.getProcessInstanceId());

                    System.out.println(" The processVariables inside here ==="+processVariables);


                    Map<String,Object> aiBriefAnalysis= (Map<String, Object>) processVariables.get("aiBriefAnalysis");

                    Boolean isComplete = (Boolean) aiBriefAnalysis.get("isComplete");

                    System.out.println(" The aiBriefAnalysis inside here ==="+aiBriefAnalysis);
                    System.out.println(" The isComplete inside here ==="+isComplete);
                    System.out.println(" The set assignee==="+setAssignee);

                    task.getProcessVariables().put("currentTask",task.getName());

                    String assignee=task.getAssignee();
                    if(assignee!=null && assignee.equalsIgnoreCase(setAssignee)){
                        task.setAssignee(String.valueOf(authenticationManager.get("name")));
                    }

                    System.out.println(" The Task Name=="+task.getName() + " and assign to " +
                            assignee);

/*                    ChatMessage chatMessage=ChatMessage.builder()
                            .content(task.getName())
                            .groupId(assignee)
                            .sender("Process Engine")
                            .build();
                    String destination = "/topic/group/" + chatMessage.getGroupId();
                    messagingTemplate.convertAndSend(destination, chatMessage);*/
/*                    System.out.println("Task Created: " + task.getName()+"" +
                            " form key== "+task.getId()
                            +" assignee=="+task.getAssignee()+
                    " the  variables===");*/
                }

            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}

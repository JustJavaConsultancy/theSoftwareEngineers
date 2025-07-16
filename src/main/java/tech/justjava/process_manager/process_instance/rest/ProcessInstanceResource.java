package tech.justjava.process_manager.process_instance.rest;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.justjava.process_manager.process_instance.model.ProcessInstanceDTO;
import tech.justjava.process_manager.process_instance.service.ProcessInstanceService;
import tech.justjava.process_manager.util.ReferencedException;
import tech.justjava.process_manager.util.ReferencedWarning;


@RestController
@RequestMapping(value = "/api/processInstances", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProcessInstanceResource {

    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceResource(final ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    @GetMapping
    public ResponseEntity<List<ProcessInstanceDTO>> getAllProcessInstances() {
        return ResponseEntity.ok(processInstanceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstance(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(processInstanceService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createProcessInstance(
            @RequestBody @Valid final ProcessInstanceDTO processInstanceDTO) {
        final Long createdId = processInstanceService.create(processInstanceDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateProcessInstance(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ProcessInstanceDTO processInstanceDTO) {
        processInstanceService.update(id, processInstanceDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteProcessInstance(@PathVariable(name = "id") final Long id) {
        final ReferencedWarning referencedWarning = processInstanceService.getReferencedWarning(id);
        if (referencedWarning != null) {
            throw new ReferencedException(referencedWarning);
        }
        processInstanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

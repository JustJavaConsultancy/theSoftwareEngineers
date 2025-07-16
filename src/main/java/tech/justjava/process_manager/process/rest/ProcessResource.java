package tech.justjava.process_manager.process.rest;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
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
import tech.justjava.process_manager.file.service.FileDataService;
import tech.justjava.process_manager.process.model.ProcessDTO;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.util.ReferencedException;
import tech.justjava.process_manager.util.ReferencedWarning;


@RestController
@RequestMapping(value = "/api/processes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProcessResource {

    private final ProcessService processService;
    private final FileDataService fileDataService;

    public ProcessResource(final ProcessService processService,
            final FileDataService fileDataService) {
        this.processService = processService;
        this.fileDataService = fileDataService;
    }

    @GetMapping
    public ResponseEntity<List<ProcessDTO>> getAllProcesses() {
        return ResponseEntity.ok(processService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessDTO> getProcess(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(processService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createProcess(@RequestBody @Valid final ProcessDTO processDTO) {
        final Long createdId = processService.create(processDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateProcess(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ProcessDTO processDTO) {
        processService.update(id, processDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteProcess(@PathVariable(name = "id") final Long id) {
        final ReferencedWarning referencedWarning = processService.getReferencedWarning(id);
        if (referencedWarning != null) {
            throw new ReferencedException(referencedWarning);
        }
        processService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/diagram/{filename}")
    public ResponseEntity<InputStreamResource> downloadDiagram(
            @PathVariable(name = "id") final Long id) {
        final ProcessDTO processDTO = processService.get(id);
        return fileDataService.provideDownload(processDTO.getDiagram());
    }

}

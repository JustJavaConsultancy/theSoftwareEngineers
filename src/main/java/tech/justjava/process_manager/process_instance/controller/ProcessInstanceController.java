package tech.justjava.process_manager.process_instance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tech.justjava.process_manager.process.domain.Process;
import tech.justjava.process_manager.process.repos.ProcessRepository;
import tech.justjava.process_manager.process_instance.model.ProcessInstanceDTO;
import tech.justjava.process_manager.process_instance.model.Status;
import tech.justjava.process_manager.process_instance.service.ProcessInstanceService;
import tech.justjava.process_manager.util.CustomCollectors;
import tech.justjava.process_manager.util.JsonStringFormatter;
import tech.justjava.process_manager.util.ReferencedWarning;
import tech.justjava.process_manager.util.WebUtils;


@Controller
@RequestMapping("/processInstances")
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;
    private final ObjectMapper objectMapper;
    private final ProcessRepository processRepository;

    public ProcessInstanceController(final ProcessInstanceService processInstanceService,
            final ObjectMapper objectMapper, final ProcessRepository processRepository) {
        this.processInstanceService = processInstanceService;
        this.objectMapper = objectMapper;
        this.processRepository = processRepository;
    }

    @InitBinder
    public void jsonFormatting(final WebDataBinder binder) {
        binder.addCustomFormatter(new JsonStringFormatter<Map<String, String>>(objectMapper) {
        }, "processVariable");
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("statusValues", Status.values());
        model.addAttribute("processValues", processRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Process::getId, Process::getModelId)));
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("processInstances", processInstanceService.findAll());
        return "processInstance/list";
    }

    @GetMapping("/add")
    public String add(
            @ModelAttribute("processInstance") final ProcessInstanceDTO processInstanceDTO) {
        return "processInstance/add";
    }

    @PostMapping("/add")
    public String add(
            @ModelAttribute("processInstance") @Valid final ProcessInstanceDTO processInstanceDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "processInstance/add";
        }
        processInstanceService.create(processInstanceDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("processInstance.create.success"));
        return "redirect:/processInstances";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final Long id, final Model model) {
        model.addAttribute("processInstance", processInstanceService.get(id));
        return "processInstance/edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable(name = "id") final Long id,
            @ModelAttribute("processInstance") @Valid final ProcessInstanceDTO processInstanceDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "processInstance/edit";
        }
        processInstanceService.update(id, processInstanceDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("processInstance.update.success"));
        return "redirect:/processInstances";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable(name = "id") final Long id,
            final RedirectAttributes redirectAttributes) {
        final ReferencedWarning referencedWarning = processInstanceService.getReferencedWarning(id);
        if (referencedWarning != null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
                    WebUtils.getMessage(referencedWarning.getKey(), referencedWarning.getParams().toArray()));
        } else {
            processInstanceService.delete(id);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("processInstance.delete.success"));
        }
        return "redirect:/processInstances";
    }

}

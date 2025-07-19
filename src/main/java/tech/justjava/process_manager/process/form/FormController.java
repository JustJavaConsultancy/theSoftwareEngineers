package tech.justjava.process_manager.process.form;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @GetMapping
    public String listForms(Model model) {
        model.addAttribute("forms", formService.findAll());
        return "form/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("form", new Form());
        return "form/create";
    }

    @PostMapping
    public String saveForm(@ModelAttribute("form") Form form) {
        formService.save(form);
        return "redirect:/forms";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        formService.findById(id).ifPresent(form -> model.addAttribute("form", form));
        return "form/edit";
    }

    @PostMapping("/update/{id}")
    public String updateForm(@PathVariable Long id, @ModelAttribute Form form) {
        form.setId(id);
        formService.save(form);
        return "redirect:/forms";
    }

    @GetMapping("/delete/{id}")
    public String deleteForm(@PathVariable Long id) {
        formService.deleteById(id);
        return "redirect:/forms";
    }
}

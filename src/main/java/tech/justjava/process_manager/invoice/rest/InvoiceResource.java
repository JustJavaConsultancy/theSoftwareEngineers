package tech.justjava.process_manager.invoice.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvoiceResource {
//
//    private final InvoiceService invoiceService;
//
//    public InvoiceResource(final InvoiceService invoiceService) {
//        this.invoiceService = invoiceService;
//    }
//
//    @GetMapping
//    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
//        return ResponseEntity.ok(invoiceService.findAll());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable(name = "id") final Long id) {
//        return ResponseEntity.ok(invoiceService.get(id));
//    }
//
//    @PostMapping
//    @ApiResponse(responseCode = "201")
//    public ResponseEntity<Long> createInvoice(@RequestBody @Valid final InvoiceDTO invoiceDTO) {
//        final Long createdId = invoiceService.create(invoiceDTO).getId();
//        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Long> updateInvoice(@PathVariable(name = "id") final Long id,
//            @RequestBody @Valid final InvoiceDTO invoiceDTO) {
//        invoiceService.update(id, invoiceDTO);
//        return ResponseEntity.ok(id);
//    }
//
//    @DeleteMapping("/{id}")
//    @ApiResponse(responseCode = "204")
//    public ResponseEntity<Void> deleteInvoice(@PathVariable(name = "id") final Long id) {
//        invoiceService.delete(id);
//        return ResponseEntity.noContent().build();
//    }

}

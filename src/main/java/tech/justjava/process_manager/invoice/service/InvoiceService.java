package tech.justjava.process_manager.invoice.service;

import org.springframework.stereotype.Service;


@Service("invoiceService")
public class InvoiceService {

//    private final InvoiceRepository invoiceRepository;
//    private final CustomerRepository customerRepository;
//    private final ProductService productService;
//    private final InvoiceMapper invoiceMapper;
//    private final TaskRepository taskRepository;
//
//    public InvoiceService(final InvoiceRepository invoiceRepository,
//                          final CustomerRepository customerRepository,
//                          ProductService productService, InvoiceMapper invoiceMapper,
//                          TaskRepository taskRepository) {
//        this.invoiceRepository = invoiceRepository;
//        this.customerRepository = customerRepository;
//        this.productService = productService;
//        this.invoiceMapper = invoiceMapper;
//        this.taskRepository = taskRepository;
//    }
//
//    public List<InvoiceDTO> findAll() {
//        final List<Invoice> invoices = invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "dueDate"));
//        return invoices.stream()
//                .map(tech.justjava.process_manager.invoice -> invoiceMapper.toDto(tech.justjava.process_manager.invoice))
//                .toList();
//    }
//    public List<InvoiceDTO> findInvoicesByMerchantId(String merchantId) {
//        final List<Invoice> invoices = invoiceRepository.findByMerchantIdOrderByDueDateDesc(merchantId);
//        return invoices.stream()
//                .map(tech.justjava.process_manager.invoice -> invoiceMapper.toDto(tech.justjava.process_manager.invoice))
//                .toList();
//    }
//    public InvoiceDTO get(final Long id) {
//        return invoiceRepository.findById(id)
//                .map(tech.justjava.process_manager.invoice -> invoiceMapper.toDto(tech.justjava.process_manager.invoice))
//                .orElseThrow(NotFoundException::new);
//    }
//
//    public InvoiceDTO createInvoice(DelegateExecution execution){
////        System.out.println("\n\n Invoice about to be created Here....."+execution.getVariables());
//
//        ProductDTO product=productService.get((Long) execution.getVariable("productId"));
//        InvoiceDTO invoiceDTO=InvoiceDTO.builder()
//                .amount(new java.math.BigDecimal(execution.getVariable("amount").toString()))
//                .customerEmail(execution.getVariable("payerEmail").toString())
//                .customerName(execution.getVariable("cardHolderName").toString())
//                .customerPhoneNumber(execution.getVariable("payerPhoneNumber").toString())
//                .description("Payment for product "+execution.getVariable("productName").toString())
//                .merchantId(execution.getVariable("merchantId").toString())
//                .issueDate(java.time.LocalDate.now())
//                .status(Status.NEW)
//                .dueDate(java.time.LocalDate.now())
//                .product(product)
//                .build();
//        invoiceDTO = create(invoiceDTO);
//        System.out.println(" Invoice Created Here....."+execution.getVariables());
//        return invoiceDTO;
//    }
//
//    public void updateStatus(DelegateExecution execution){
//        System.out.println(" Updating status here....");
//    }
//    public InvoiceDTO create(final InvoiceDTO invoiceDTO) {
//        Invoice tech.justjava.process_manager.invoice = new Invoice();
//        tech.justjava.process_manager.invoice = invoiceMapper.toEntity(invoiceDTO);
//        tech.justjava.process_manager.invoice= invoiceRepository.save(tech.justjava.process_manager.invoice);
//        return invoiceMapper.toDto(tech.justjava.process_manager.invoice);
//    }
//
//    public void update(final Long id, final InvoiceDTO invoiceDTO) {
//        Invoice tech.justjava.process_manager.invoice = invoiceRepository.findById(id)
//                .orElseThrow(NotFoundException::new);
//        tech.justjava.process_manager.invoice = invoiceMapper.toEntity(invoiceDTO);
//        invoiceRepository.save(tech.justjava.process_manager.invoice);
//    }
//
//    public void delete(final Long id) {
//        invoiceRepository.deleteById(id);
//    }
//
//    public Task getInvoiceTaskByInstanceAndDefinitionKey(String processInstanceId,
//                                                         String taskDefinitionKey){
//
//        return taskRepository.getTaskByInstanceAndDefinitionKey(processInstanceId, taskDefinitionKey);
//    }
//
//    public Task getInvoiceTaskById(String id){
//        return taskRepository.getTaskById(id);
//    }
/*    private InvoiceDTO mapToDTO(final Invoice tech.justjava.process_manager.invoice, final InvoiceDTO invoiceDTO) {
        invoiceDTO.setId(tech.justjava.process_manager.invoice.getId());
        invoiceDTO.setIssueDate(tech.justjava.process_manager.invoice.getIssueDate());
        invoiceDTO.setDueDate(tech.justjava.process_manager.invoice.getDueDate());
        invoiceDTO.setAmount(tech.justjava.process_manager.invoice.getAmount());
        invoiceDTO.setCustomerEmail(tech.justjava.process_manager.invoice.getCustomerEmail());
        invoiceDTO.setCustomerName(tech.justjava.process_manager.invoice.getCustomerName());
        invoiceDTO.setCustomerPhoneNumber(tech.justjava.process_manager.invoice.getCustomerPhoneNumber());
        invoiceDTO.setDescription(tech.justjava.process_manager.invoice.getDescription());
        invoiceDTO.setMerchantId(tech.justjava.process_manager.invoice.getMerchantId());
        invoiceDTO.setStatus(tech.justjava.process_manager.invoice.getStatus());
        invoiceDTO.setDateCreated(tech.justjava.process_manager.invoice.getDateCreated());
        invoiceDTO.setLastUpdated(tech.justjava.process_manager.invoice.getLastUpdated());
        invoiceDTO.setProduct(tech.justjava.process_manager.invoice.getProduct());
        return invoiceDTO;
    }*/

/*    private Invoice mapToEntity(final InvoiceDTO invoiceDTO, final Invoice tech.justjava.process_manager.invoice) {
        tech.justjava.process_manager.invoice.setCustomerEmail(invoiceDTO.getCustomerEmail());
        tech.justjava.process_manager.invoice.setCustomerName(invoiceDTO.getCustomerName());
        tech.justjava.process_manager.invoice.setCustomerPhoneNumber(invoiceDTO.getCustomerPhoneNumber());
        tech.justjava.process_manager.invoice.setDescription(invoiceDTO.getDescription());
        tech.justjava.process_manager.invoice.setMerchantId(invoiceDTO.getMerchantId());
        tech.justjava.process_manager.invoice.setIssueDate(invoiceDTO.getIssueDate());
        tech.justjava.process_manager.invoice.setDueDate(invoiceDTO.getDueDate());
        tech.justjava.process_manager.invoice.setAmount(invoiceDTO.getAmount());
        tech.justjava.process_manager.invoice.setStatus(invoiceDTO.getStatus());
        return tech.justjava.process_manager.invoice;
    }*/

}

package tech.justjava.process_manager.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.justjava.process_manager.account.AuthenticationManager;
import tech.justjava.process_manager.process.service.ProcessService;
import tech.justjava.process_manager.task.repos.TaskRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component("paymentService")
public class PaymentService {

    private final ProcessService processService;
//    private final ProductService productService;
//    private final AccountMapper accountMapper;
//    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final TaskRepository taskRepository;
//    private final TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    AmqpTemplate rabbitTemplate;

    @Value("${message.paymentGateway-routing-key}")
    String paymentRoutingKey;

    public PaymentService(ProcessService processService,
                          AuthenticationManager authenticationManager, TaskRepository taskRepository
                          ) {
        this.processService = processService;
//        this.productService = productService;
//        this.accountMapper = accountMapper;
//        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.taskRepository = taskRepository;
//        this.transactionService = transactionService;
    }

//    nibss transfer
//    public void nibssTransfer(DelegateExecution execution){
//        System.out.println("This is nibss transfer" + execution);
//    }

//    public void settlement(DelegateExecution execution){
//        Map<String, Object> variables = execution.getVariables();
//        String paymentAction = (String) variables.get("paymentAction");
//
//
//        if("buy".equalsIgnoreCase(paymentAction)){
////            Transaction transaction = (Transaction) variables.get("transaction");
//            Long productId = (Long) variables.get("productId");
//            ProductDTO product = productService.get(productId);
//
//            Long accountId = product.getAccountId();
//            AccountDTO creditAccountDTO = accountService.get(String.valueOf(accountId));
//            Account creditAccount = accountMapper.toEntity(creditAccountDTO);
//            String merchantId = creditAccountDTO.getOwnerId();
//            Account payableAccount = accountService.getMerchantPayableAccount(merchantId);
//            String uuid = UUID.randomUUID().toString();
//            String referenceNo = String.valueOf(Math.abs(uuid.hashCode()));
//            Map<String, Object> transactionDetails = new HashMap<>();
//
//            TransactionDTO transactionDTO=TransactionDTO.builder()
//                    .amount((BigDecimal) payableAccount.getBalance())
//                    .beneficiaryAccount(creditAccount.getName())
//                    .reference(referenceNo)
//                    .externalReference(referenceNo)
//                    .paymentType(PaymentType.INFLOW)
//                    .channel("settlement")
//                    .sourceAccount(payableAccount.getName())
//                    .transactionOwner(merchantId)
////                .tech.justjava.process_manager.invoice(invoiceDTO)
//                    .transactionDetails(transactionDetails)
//                    .status(Status.PAID)
//                    .build();
//
//            Transaction transaction=transactionService.createEntity(transactionDTO);
//            System.out.println("The payable account amount is " + payableAccount);
//
//            accountService.debitCreditForSettlement(payableAccount, creditAccount, transaction);
//        } else {
//            System.out.println("The execution while settlement=="+execution);
//        }
//
//    }
//    public void reconciliation(DelegateExecution execution){
//        System.out.println("The execution while reconciliation=="+execution);
//    }

//    tech.justjava.process_manager.payment outflow process
//    public void startPaymentOutflowProcess(Map<String, Object> variables, String merchantId){
//        processService.startProcessByMessageStartEvent(merchantId, "processOutflowPayment", variables);
//    }

    public void startPaymentProcess(Map<String,Object> variables){
//        System.out.println("\n\nThese are the tech.justjava.process_manager.payment variables == " + variables);
        rabbitTemplate.convertAndSend(
                "flowable.message.exchange",  // Exchange
                paymentRoutingKey, // Routing key
                variables);
//        processService.startProcessByMessageStartEvent(merchantId,
//                "processPayment",variables);
    }

    public void startTopupProcessWithCard(Map<String, String> cardInfo){
//        Map<String, Object> variables = new HashMap<>();
        String formattedAmount = cardInfo.get("amount");
        String cleanAmount = formattedAmount.replaceAll(",", "");
        BigDecimal amount = BigDecimal.valueOf(Long.parseLong(cleanAmount));
        String customerName = authenticationManager.get("given_name").toString() + " " +
                authenticationManager.get("family_name").toString();

//        PaymentDTO paymentDTO=PaymentDTO.builder()
//                .amount(amount)
//                .cardCvv(cardInfo.get("cvv"))
//                .channel("card")
//                .cardExpirationDate(cardInfo.get("expiryDate"))
//                .cardHolderName(customerName)
//                .invoiceId(1L)
//                .cardNumber(cardInfo.get("cardNumber"))
//                .currency("NIG")
//                .payerEmail(authenticationManager.get("email").toString())
//                .payerPhoneNumber(authenticationManager.get("phoneNumber") != null? authenticationManager.get("phoneNumber").toString(): "")
//                .build();
//
//        Map<String,Object> variables=objectMapper.convertValue(paymentDTO,Map.class);
        Map<String, Object> variables = new HashMap<>();
        variables.put("merchantId",authenticationManager.get("sub").toString());
        variables.put("paymentAction", "topup");

//        System.out.println("This is tech.justjava.process_manager.payment variables" + variables);
        startPaymentProcess(variables);
    }

    public void startPurchaseProcessWithCard(Map<String, String> cardInfo, Map<String, Object> invoice,
                                             String payTaskId){
//        System.out.println("Inside startPurchaseProcessWithCard==cardInfo=="+ cardInfo);
        System.out.println("This is the tech.justjava.process_manager.invoice" + invoice);

        BigDecimal amount = new BigDecimal(String.valueOf(invoice.get("amount")));

        Map<String, Object> product = (Map<String, Object>) invoice.get("product");
//        PaymentDTO paymentDTO=PaymentDTO.builder()
//                .amount(amount)
//                .cardCvv(cardInfo.get("cvv"))
//                .channel("card")
//                .cardExpirationDate(cardInfo.get("expiry-date"))
//                .cardHolderName((String) tech.justjava.process_manager.invoice.get("customerName"))
//                .invoiceId(1L)
//                .cardNumber( cardInfo.get("card-number"))
//                .currency("NIG")
//                .payerEmail((String) tech.justjava.process_manager.invoice.get("customerEmail"))
//                .payerPhoneNumber((String) tech.justjava.process_manager.invoice.get("customerPhoneNumber"))
//                .build();

//        Map<String,Object> variables=objectMapper.convertValue(paymentDTO,Map.class);
        Map<String, Object> variables = new HashMap<>();
        variables.put("productName",product.get("name"));
        variables.put("paymentAction", "buy");
        variables.put("productId",product.get("id"));
        variables.put("merchantId",product.get("merchantId"));
        variables.put("quantityBought", invoice.get("quantity"));

//        System.out.println("This is the purchase ::" + variables);
//        taskRepository.completeTask(payTaskId,variables);
//        startPaymentProcess(variables);
    }
}


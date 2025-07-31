package tech.justjava.process_manager.invoice.domain;

import tech.justjava.process_manager.invoice.model.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(name = "Invoices")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString
public class Invoice implements Serializable {


    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "customerEmail")
    private String customerEmail;

    @Column(name = "customerName")
    private String customerName;
    @Column(name = "merchantId")
    private String merchantId;
    @Column(name = "customerPhoneNumber")
    private String customerPhoneNumber;

    private String quantity;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

//    @ToString.Exclude
//    @ManyToOne
//    @JoinColumn(name = "product_id", nullable = true)
//    private Product product;
}

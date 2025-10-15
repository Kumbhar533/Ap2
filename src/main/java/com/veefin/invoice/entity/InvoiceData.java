package com.veefin.invoice.entity;

import com.veefin.invoice.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Table(name = "invoice_data")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceData {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 50, nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "file_name")
    private String fileName;
    @Column(name = "merchant_name")
    private String merchantName;
    @Column(name = "invoice_number")
    private String invoiceNumber;
    @Column(name = "total_amount")
    private Double totalAmount;
    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @ElementCollection
    @CollectionTable(name = "invoice_field_map", joinColumns = @JoinColumn(name = "invoice_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "field_value")
    private Map<String, String> fieldMap; // JSON-style key-value pairs

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        }
    }
}

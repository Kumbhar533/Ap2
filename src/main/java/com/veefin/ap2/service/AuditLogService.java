package com.veefin.ap2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veefin.ap2.entity.AP2AuditLog;
import com.veefin.ap2.repository.AP2AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AP2AuditRepository auditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log Intent Mandate events
     */
    public void logIntentEvent(String action, String intentHash, String invoiceUuid,
                               String actor, boolean success, String details, Double amount, String merchantName) {
        try {
            AP2AuditLog audit = AP2AuditLog.builder()
                    .mandateType(AP2AuditLog.MandateType.INTENT)
                    .mandateId(intentHash)
                    .invoiceUuid(invoiceUuid)
                    .action(AP2AuditLog.AuditAction.valueOf(action.toUpperCase()))
                    .actor(actor)
                    .status(success ? AP2AuditLog.AuditStatus.SUCCESS : AP2AuditLog.AuditStatus.FAILURE)
                    .details(details)
                    .amount(amount)
                    .merchantName(merchantName)
                    .build();

            auditRepository.save(audit);
            log.info("🔍 AUDIT: Intent {} - {} by {} - {}", action, intentHash, actor, success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    /**
     * Log Cart Mandate events
     */
    public void logCartEvent(String action, String cartId, String invoiceUuid,
                             String actor, boolean success, String details, Double amount, String merchantName) {
        try {
            AP2AuditLog audit = AP2AuditLog.builder()
                    .mandateType(AP2AuditLog.MandateType.CART)
                    .mandateId(cartId)
                    .invoiceUuid(invoiceUuid)
                    .action(AP2AuditLog.AuditAction.valueOf(action.toUpperCase()))
                    .actor(actor)
                    .status(success ? AP2AuditLog.AuditStatus.SUCCESS : AP2AuditLog.AuditStatus.FAILURE)
                    .details(details)
                    .amount(amount)
                    .merchantName(merchantName)
                    .build();

            auditRepository.save(audit);
            log.info("🔍 AUDIT: Cart {} - {} by {} - {}", action, cartId, actor, success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    /**
     * Log Payment Mandate events
     */
    public void logPaymentEvent(String action, String paymentId, String invoiceUuid,
                                String actor, boolean success, String details, Double amount, String merchantName) {
        try {
            AP2AuditLog audit = AP2AuditLog.builder()
                    .mandateType(AP2AuditLog.MandateType.PAYMENT)
                    .mandateId(paymentId)
                    .invoiceUuid(invoiceUuid)
                    .action(AP2AuditLog.AuditAction.valueOf(action.toUpperCase()))
                    .actor(actor)
                    .status(success ? AP2AuditLog.AuditStatus.SUCCESS : AP2AuditLog.AuditStatus.FAILURE)
                    .details(details)
                    .amount(amount)
                    .merchantName(merchantName)
                    .build();

            auditRepository.save(audit);
            log.info("🔍 AUDIT: Payment {} - {} by {} - {}", action, paymentId, actor, success ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    /**
     * Log signature verification events
     */
    public void logSignatureVerification(AP2AuditLog.MandateType mandateType, String mandateId,
                                         String actor, boolean verified, String signatureHash) {
        try {
            String details = String.format("Signature verification %s for %s",
                    verified ? "PASSED" : "FAILED", mandateType);

            AP2AuditLog audit = AP2AuditLog.builder()
                    .mandateType(mandateType)
                    .mandateId(mandateId)
                    .action(AP2AuditLog.AuditAction.VERIFY)
                    .actor(actor)
                    .status(verified ? AP2AuditLog.AuditStatus.SUCCESS : AP2AuditLog.AuditStatus.FAILURE)
                    .details(details)
                    .signatureHash(signatureHash)
                    .build();

            auditRepository.save(audit);
            log.info("🔍 AUDIT: Signature verification - {} - {}", mandateId, verified ? "SUCCESS" : "FAILURE");
        } catch (Exception e) {
            log.error("Failed to log signature verification: {}", e.getMessage());
        }
    }


}
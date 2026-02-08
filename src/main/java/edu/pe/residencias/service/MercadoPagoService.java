package edu.pe.residencias.service;

import edu.pe.residencias.dto.payment.CreatePaymentRequest;
import edu.pe.residencias.dto.payment.CreatePaymentResponse;

public interface MercadoPagoService {
    CreatePaymentResponse createPreference(CreatePaymentRequest request);
    boolean verifyPreferenceAndMarkResidencia(String preferenceId, Long residenciaId);
    boolean handlePaymentNotification(String paymentId);
    // New methods for SDK/RestTemplate operations
    com.fasterxml.jackson.databind.JsonNode getMerchantOrder(Long id);
    com.fasterxml.jackson.databind.JsonNode getPayment(Long paymentId);
    edu.pe.residencias.dto.payment.CreatePaymentResponse createCheckoutPreference(java.math.BigDecimal amount, String title, String externalReference, String payerEmail, String dni);
}

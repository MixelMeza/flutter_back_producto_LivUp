package edu.pe.residencias.service;

import edu.pe.residencias.dto.payment.CreatePaymentRequest;
import edu.pe.residencias.dto.payment.CreatePaymentResponse;

public interface MercadoPagoService {
    CreatePaymentResponse createPreference(CreatePaymentRequest request);
    boolean verifyPreferenceAndMarkResidencia(String preferenceId, Long residenciaId);
    boolean handlePaymentNotification(String paymentId);
}

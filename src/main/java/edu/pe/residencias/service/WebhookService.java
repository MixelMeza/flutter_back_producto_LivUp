package edu.pe.residencias.service;

public interface WebhookService {
    /**
     * Process a payment notification from Mercado Pago. Implementation must handle
     * errors internally and not throw exceptions that cause 5xx responses.
     * @param paymentId Mercado Pago payment id
     */
    void processPaymentNotification(String paymentId);

    /**
     * Process a merchant_order notification: fetch merchant_order, iterate payments and
     * log details. Should mark residencia as destacado when external_reference matches.
     */
    void processMerchantOrderNotification(String merchantOrderId);
}

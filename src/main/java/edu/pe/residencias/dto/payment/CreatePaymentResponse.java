package edu.pe.residencias.dto.payment;

public class CreatePaymentResponse {
    private String initPoint;
    private String preferenceId;

    public CreatePaymentResponse() {
    }

    public CreatePaymentResponse(String initPoint, String preferenceId) {
        this.initPoint = initPoint;
        this.preferenceId = preferenceId;
    }

    public String getInitPoint() {
        return initPoint;
    }

    public void setInitPoint(String initPoint) {
        this.initPoint = initPoint;
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }
}

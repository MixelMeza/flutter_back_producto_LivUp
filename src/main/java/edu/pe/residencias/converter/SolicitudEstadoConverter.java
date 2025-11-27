package edu.pe.residencias.converter;

import edu.pe.residencias.model.enums.SolicitudEstado;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SolicitudEstadoConverter implements AttributeConverter<SolicitudEstado, String> {

    @Override
    public String convertToDatabaseColumn(SolicitudEstado attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValor();
    }

    @Override
    public SolicitudEstado convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return SolicitudEstado.fromValor(dbData);
    }
}

package edu.pe.residencias.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import edu.pe.residencias.model.enums.SolicitudEstado;

@Converter(autoApply = false)
public class SolicitudEstadoConverter implements AttributeConverter<SolicitudEstado, String> {

    @Override
    public String convertToDatabaseColumn(SolicitudEstado attribute) {
        if (attribute == null) return null;
        // store the 'valor' (e.g., "aceptada") to keep compatibility with existing DB values
        return attribute.getValor();
    }

    @Override
    public SolicitudEstado convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // fromValor is case-insensitive
        return SolicitudEstado.fromValor(dbData);
    }
}

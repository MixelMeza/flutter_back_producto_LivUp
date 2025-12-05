package edu.pe.residencias.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import edu.pe.residencias.model.enums.HabitacionEstado;

@Converter(autoApply = false)
public class HabitacionEstadoConverter implements AttributeConverter<HabitacionEstado, String> {

    @Override
    public String convertToDatabaseColumn(HabitacionEstado attribute) {
        if (attribute == null) return null;
        // store as lowercase without spaces
        return attribute.name().toLowerCase().replace('_', '-');
    }

    @Override
    public HabitacionEstado convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        HabitacionEstado he = HabitacionEstado.fromString(dbData);
        return he;
    }
}

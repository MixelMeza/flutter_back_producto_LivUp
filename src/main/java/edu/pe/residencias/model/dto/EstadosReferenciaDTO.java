package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadosReferenciaDTO {
    private List<Map<String, String>> contratos;
    private List<Map<String, String>> solicitudes;
    private List<Map<String, String>> usuarios;
    private List<Map<String, String>> pagos;
    private List<Map<String, String>> residencias;

    public static EstadosReferenciaDTO crear() {
        EstadosReferenciaDTO dto = new EstadosReferenciaDTO();
        
        // Estados de Contratos
        dto.contratos = List.of(
            Map.of("valor", "pendiente_inicio", "label", "Pendiente de Inicio"),
            Map.of("valor", "vigente", "label", "Vigente"),
            Map.of("valor", "vencido", "label", "Vencido"),
            Map.of("valor", "cancelado", "label", "Cancelado"),
            Map.of("valor", "renovado", "label", "Renovado"),
            Map.of("valor", "finalizado", "label", "Finalizado")
        );
        
        // Estados de Solicitudes
        dto.solicitudes = List.of(
            Map.of("valor", "pendiente", "label", "Pendiente"),
            Map.of("valor", "reservada", "label", "Reservada"),
            Map.of("valor", "aceptada", "label", "Aceptada"),
            Map.of("valor", "rechazada", "label", "Rechazada"),
            Map.of("valor", "cancelada", "label", "Cancelada")
        );
        
        // Estados de Usuarios
        dto.usuarios = List.of(
            Map.of("valor", "activo", "label", "Activo"),
            Map.of("valor", "inactivo", "label", "Inactivo"),
            Map.of("valor", "suspendido", "label", "Suspendido")
        );
        
        // Estados de Pagos
        dto.pagos = List.of(
            Map.of("valor", "pagado", "label", "Pagado"),
            Map.of("valor", "parcial", "label", "Parcial"),
            Map.of("valor", "pendiente", "label", "Pendiente"),
            Map.of("valor", "proximo", "label", "Próximo"),
            Map.of("valor", "vencido", "label", "Vencido"),
            Map.of("valor", "anulado", "label", "Anulado")
        );
        
        // Estados de Residencias
        dto.residencias = List.of(
            Map.of("valor", "Activo", "label", "Activo"),
            Map.of("valor", "Inactivo", "label", "Inactivo"),
            Map.of("valor", "Mantenimiento", "label", "En Mantenimiento")
        );
        
        return dto;
    }
}

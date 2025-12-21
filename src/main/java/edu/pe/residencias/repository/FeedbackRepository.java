package edu.pe.residencias.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEstado(Feedback.Estado estado);
    List<Feedback> findByTipo(Feedback.Tipo tipo);
}

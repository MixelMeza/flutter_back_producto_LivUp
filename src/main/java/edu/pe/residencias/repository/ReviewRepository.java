package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
}

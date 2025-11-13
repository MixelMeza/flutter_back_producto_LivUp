package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Review;

public interface ReviewService {
    Review create(Review review);
    Review update(Review review);
    void delete(Long id);
    Optional<Review> read(Long id);
    List<Review> readAll();
}

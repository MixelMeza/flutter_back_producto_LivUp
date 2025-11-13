package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Review;
import edu.pe.residencias.repository.ReviewRepository;
import edu.pe.residencias.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository repository;

    @Override
    public Review create(Review review) {
        return repository.save(review);
    }

    @Override
    public Review update(Review review) {
        return repository.save(review);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Review> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Review> readAll() {
        return repository.findAll();
    }
}

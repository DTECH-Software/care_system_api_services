package com.dtech.auth.repository;

import com.dtech.auth.model.DocumentStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentStoreRepository extends JpaRepository<DocumentStore, Long> {
    Optional<DocumentStore> findByCode(String code);
}

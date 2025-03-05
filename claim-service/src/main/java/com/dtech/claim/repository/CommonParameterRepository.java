package com.dtech.claim.repository;

import com.dtech.claim.model.CommonParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CommonParameterRepository extends JpaRepository<CommonParameter, Long> {
   Optional<CommonParameter> findByCode(String code);
}

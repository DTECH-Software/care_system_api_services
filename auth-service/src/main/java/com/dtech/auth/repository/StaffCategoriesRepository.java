package com.dtech.auth.repository;

import com.dtech.auth.enums.Status;
import com.dtech.auth.model.StaffCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffCategoriesRepository extends JpaRepository<StaffCategories, Long> {
    Optional<StaffCategories> findByCodeAndStatus(String code, Status status);
}

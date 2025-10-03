/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 7:58 PM
 * <p>
 */

package com.dtech.auth.repository;

import com.dtech.auth.enums.RelationCategory;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimDependentsRepository extends JpaRepository<ClaimsDependents,Long> {
    boolean existsAllByApplicationUserAndRelationCategoryAndStatusIn(ApplicationUser applicationUser, RelationCategory relationCategory, List<Workflow> workflow);
    boolean existsAllByApplicationUserAndRelationCategoryAndStatusInAndMarried_Id(ApplicationUser applicationUser, RelationCategory relationCategory, List<Workflow> workflow,Long id);
    List<ClaimsDependents> findAllByStatusIn(List<Workflow> workflow);
}

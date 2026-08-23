/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 7:58 PM
 * <p>
 */

package com.dtech.message.repository;


import com.dtech.message.enums.RelationCategory;
import com.dtech.message.enums.Workflow;
import com.dtech.message.model.ApplicationUser;
import com.dtech.message.model.ClaimsDependents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimDependentsRepository extends JpaRepository<ClaimsDependents,Long> {
    List<ClaimsDependents> findAllByApplicationUserAndRelationCategoryAndStatusIn(ApplicationUser applicationUser, RelationCategory relationCategory, List<Workflow> workflow);
    List<ClaimsDependents> findAllByApplicationUser(ApplicationUser applicationUser);
}

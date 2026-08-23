package com.dtech.auth.validator.validators;

import com.dtech.auth.dto.request.ClaimDependentDetailsRequestDTO;
import com.dtech.auth.dto.request.validator.ClaimDependentDetailsRequestValidatorDTO;
import com.dtech.auth.enums.RelationCategory;
import com.dtech.auth.validator.NoDuplicateRelationCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.log4j.Log4j2;

import java.util.*;

@Log4j2
public class NoDuplicateRelationCategoryValidators implements ConstraintValidator<NoDuplicateRelationCategory, List<ClaimDependentDetailsRequestValidatorDTO>> {

    @Override
    public void initialize(NoDuplicateRelationCategory constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(List<ClaimDependentDetailsRequestValidatorDTO> dependents, ConstraintValidatorContext context) {
        log.info("Call check duplicate relation categories {}", dependents);
        if (dependents == null || dependents.isEmpty()) {
            return true;
        }

        Map<String, String> seenRelationCategories = new HashMap<>();

        for (ClaimDependentDetailsRequestValidatorDTO dependent : dependents) {
            String relationCategory = dependent.getRelationCategory();

            if (RelationCategory.MOTHER.name().equals(relationCategory)
                    || RelationCategory.FATHER.name().equals(relationCategory)
                    || RelationCategory.WIFE.name().equals(relationCategory)
                    || RelationCategory.HUSBAND.name().equals(relationCategory)
                    || RelationCategory.MOTHER_IN_LAW.name().equals(relationCategory)
                    || RelationCategory.FATHER_IN_LAW.name().equals(relationCategory)) {

                String marriedStatus = "";
                if (RelationCategory.MOTHER_IN_LAW.name().equals(relationCategory) ||
                        RelationCategory.FATHER_IN_LAW.name().equals(relationCategory)
                        || RelationCategory.WIFE.name().equals(relationCategory)
                        || RelationCategory.HUSBAND.name().equals(relationCategory)
                ) {
                    log.info("Relation category found {}", relationCategory);
                    marriedStatus = dependent.getMarried();
                }

                String uniqueKey = relationCategory + " " + marriedStatus;
                if (seenRelationCategories.containsKey(uniqueKey)) {
                    log.info("Duplicate relation category {}", uniqueKey);
                    return false;
                }

                seenRelationCategories.put(uniqueKey, relationCategory);

            }

        }
        return true;
    }
}
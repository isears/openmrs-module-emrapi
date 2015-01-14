/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.emrapi.conditionlist.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.conditionlist.dao.ConditionDAO;
import org.openmrs.module.emrapi.conditionlist.domain.Condition;
import org.openmrs.module.emrapi.conditionlist.util.ConditionListConstants;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.List;

@Handler(supports = {Condition.class})
public class ConditionValidator implements Validator {
    /**
     * Log for this class and subclasses
     */
    protected final Log log = LogFactory.getLog(getClass());

    private AdministrationService administrationService;
    private ConditionDAO conditionDao;

    public ConditionValidator(ConditionDAO conditionDAO, AdministrationService administrationService) {
        this.conditionDao = conditionDAO;
        this.administrationService = administrationService;
    }

    /**
     * Determines if the command object being submitted is a valid type
     *
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public boolean supports(Class c) {
        return Condition.class.isAssignableFrom(c);
    }

    @Override
    public void validate(Object obj, Errors errors) {

        Condition condition = (Condition) obj;
        if (condition == null) {
            errors.reject("error.general");
        } else {
            // for the following elements Condition.hbm.xml says: not-null="true"
            ValidationUtils.rejectIfEmpty(errors, "patient", "error.null");
            ValidationUtils.rejectIfEmpty(errors, "status", "error.null");
            ValidationUtils.rejectIfEmpty(errors, "creator", "error.null");
            ValidationUtils.rejectIfEmpty(errors, "concept", "error.null");
            ValidationUtils.rejectIfEmpty(errors, "dateCreated", "error.null");
            ValidationUtils.rejectIfEmpty(errors, "uuid", "error.null");

            validateNonCodedCondition(condition, errors);
            validateUpdatingConcept(condition, errors);
            validateDuplicateConditions(condition, errors);
        }
    }

    private void validateDuplicateConditions(Condition condition, Errors errors) {
        List<Condition> conditionsForPatient = conditionDao.getConditionsByPatient(condition.getPatient());
        if (condition.getConditionNonCoded() != null) {
            for (Condition eachCondition : conditionsForPatient) {
                if (eachCondition.getConcept().equals(condition.getConcept())
                        && eachCondition.getConditionNonCoded().equalsIgnoreCase(condition.getConditionNonCoded().replaceAll("\\s", "")) && !eachCondition.getUuid().equals(condition.getUuid())) {
                    errors.rejectValue("concept", "Condition.error.duplicatesNotAllowed");
                }
            }
        }
    }

    private void validateUpdatingConcept(Condition condition, Errors errors) {
        Condition existingCondition = conditionDao.getConditionByUuid(condition.getUuid());
        if (existingCondition != null && !condition.getConcept().equals(existingCondition.getConcept())) {
            errors.rejectValue("concept", "Condition.error.conceptsCannotBeUpdated");
        }
    }

    private void validateNonCodedCondition(Condition condition, Errors errors) {
            String nonCodedConditionUuid = administrationService.getGlobalProperty(ConditionListConstants.GLOBAL_PROPERTY_NON_CODED_UUID);
        if (condition.getConditionNonCoded() != null) {
            if (!condition.getConcept().getUuid().equals(nonCodedConditionUuid)) {
                errors.rejectValue("conditionNonCoded", "Condition.error.conditionNonCodedValueNotSupportedForCodedCondition");
            }
        } else {
            if (condition.getConcept().getUuid().equals(nonCodedConditionUuid)) {
                errors.rejectValue("conditionNonCoded", "Condition.error.conditionNonCodedValueNeededForNonCodedCondition");
            }
        }
    }
}

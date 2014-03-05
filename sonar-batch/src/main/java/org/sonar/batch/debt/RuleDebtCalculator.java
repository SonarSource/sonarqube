/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.debt;

import com.google.common.base.Objects;
import org.sonar.api.BatchExtension;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.internal.WorkDuration;
import org.sonar.api.utils.internal.WorkDurationFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Computes the remediation cost based on the quality and analysis models.
 */
public class RuleDebtCalculator implements BatchExtension {

  private TechnicalDebtModel model;
  private final WorkDurationFactory workDurationFactory;

  public RuleDebtCalculator(TechnicalDebtModel model, WorkDurationFactory workDurationFactory) {
    this.model = model;
    this.workDurationFactory = workDurationFactory;
  }

  /**
   * Calculate the technical debt from a requirement
   */
  @CheckForNull
  public WorkDuration calculateTechnicalDebt(RuleKey ruleKey, @Nullable Double effortToFix) {
    Requirement requirement = model.requirementsByRule(ruleKey);
    if (requirement != null) {
      if (requirement.function().equals(DefaultRequirement.CONSTANT_ISSUE) && effortToFix != null) {
        throw new IllegalArgumentException("Requirement for '" + ruleKey + "' can not use 'Constant/issue' remediation function " +
          "because this rule does not have a fixed remediation cost.");
      }
      return calculateTechnicalDebt(requirement, effortToFix);
    }
    return null;
  }

  private WorkDuration calculateTechnicalDebt(Requirement requirement, @Nullable Double effortToFix) {
    WorkDuration result = workDurationFactory.createFromWorkingValue(0, WorkDuration.UNIT.DAYS);

    int factorValue = requirement.factorValue();
    if (factorValue > 0) {
      int effortToFixValue = Objects.firstNonNull(effortToFix, 1).intValue();
      result = workDurationFactory.createFromWorkingValue(factorValue, requirement.factorUnit()).multiply(effortToFixValue);
    }

    int offsetValue = requirement.offsetValue();
    if (offsetValue > 0) {
      result = result.add(workDurationFactory.createFromWorkingValue(offsetValue, requirement.offsetUnit()));
    }
    return result;
  }

}

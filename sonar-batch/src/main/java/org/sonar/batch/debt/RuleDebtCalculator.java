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
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkDuration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Computes the remediation cost based on the quality and analysis models.
 */
public class RuleDebtCalculator implements BatchExtension {

  private final TechnicalDebtModel model;
  private final int hoursInDay;

  public RuleDebtCalculator(TechnicalDebtModel model, Settings settings) {
    this.model = model;
    this.hoursInDay = settings.getInt(CoreProperties.HOURS_IN_DAY);
  }

  /**
   * Calculate the technical debt from a requirement
   */
  @CheckForNull
  public Long calculateTechnicalDebt(RuleKey ruleKey, @Nullable Double effortToFix) {
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

  private long calculateTechnicalDebt(Requirement requirement, @Nullable Double effortToFix) {
    long result = 0L;

    int factorValue = requirement.factorValue();
    if (factorValue > 0) {
      int effortToFixValue = Objects.firstNonNull(effortToFix, 1).intValue();
      result = convertValueAndUnitToSeconds(factorValue, requirement.factorUnit()) * effortToFixValue;
    }

    int offsetValue = requirement.offsetValue();
    if (offsetValue > 0) {
      result += convertValueAndUnitToSeconds(offsetValue, requirement.offsetUnit());
    }
    return result;
  }

  private int convertValueAndUnitToSeconds(int value, WorkDuration.UNIT unit){
    if (WorkDuration.UNIT.DAYS.equals(unit)) {
      return value * hoursInDay * 60 * 60;
    } else if (WorkDuration.UNIT.HOURS.equals(unit)) {
      return value * 60 * 60;
    } else if (WorkDuration.UNIT.MINUTES.equals(unit)) {
      return value * 60;
    }
    throw new IllegalStateException("Invalid unit : " + unit);
  }

}

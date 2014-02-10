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
package org.sonar.batch.technicaldebt;

import com.google.common.base.Objects;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkUnit;

/**
 * Computes the remediation cost based on the quality and analysis models.
 */
public class TechnicalDebtCalculator implements BatchExtension {

  private int hoursInDay;

  private TechnicalDebtModel model;

  public TechnicalDebtCalculator(TechnicalDebtModel model, Settings settings) {
    this.model = model;
    this.hoursInDay = settings.getInt(CoreProperties.HOURS_IN_DAY);
  }

  /**
   * Get the technical debt from the requirement
   */
  public WorkUnit calculTechnicalDebt(Issue issue) {
    Requirement requirement = model.requirementsByRule(issue.ruleKey());
    if (requirement != null) {
      if (requirement.function().equals(DefaultRequirement.CONSTANT_ISSUE) && issue.effortToFix() != null) {
        throw new IllegalArgumentException("Requirement for '" + issue.ruleKey() + "' can not use 'Constant/issue' remediation function " +
          "because this rule does not have a fixed remediation cost.");
      }
      return fromMinutes(calculTechnicalDebt(requirement, issue));
    }
    return null;
  }

  private long calculTechnicalDebt(Requirement requirement, Issue issue) {
    long effortToFix = Objects.firstNonNull(issue.effortToFix(), 1L).longValue();

    WorkUnit factorUnit = requirement.factor();
    long factor = factorUnit != null ? toMinutes(factorUnit) : 0L;

    WorkUnit offsetUnit = requirement.offset();
    long offset = offsetUnit != null ? toMinutes(offsetUnit) : 0L;

    return effortToFix * factor + offset;
  }

  private long toMinutes(WorkUnit factor) {
    if (factor.days() > 0) {
      return Double.valueOf(factor.days() * hoursInDay * 60d).longValue();
    } else if (factor.hours() > 0) {
      return Double.valueOf(factor.hours() * 60d).longValue();
    } else {
      return Double.valueOf(factor.minutes()).longValue();
    }
  }

  private WorkUnit fromMinutes(Long inMinutes) {
    int oneHourInMinute = 60;
    int days = 0;
    int hours = 0;
    int minutes = 0;

    int oneWorkingDay = hoursInDay * oneHourInMinute;
    if (inMinutes >= oneWorkingDay) {
      Long nbDays = inMinutes / oneWorkingDay;
      days = nbDays.shortValue();
      inMinutes = inMinutes - (nbDays * oneWorkingDay);
    }

    if (inMinutes >= oneHourInMinute) {
      Long nbHours = inMinutes / oneHourInMinute;
      hours = nbHours.shortValue();
      inMinutes = inMinutes - (nbHours * oneHourInMinute);
    }

    minutes = inMinutes.shortValue();

    return new WorkUnit.Builder().setDays(days).setHours(hours).setMinutes(minutes).build();
  }
}

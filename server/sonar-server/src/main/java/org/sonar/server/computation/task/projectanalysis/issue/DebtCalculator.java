/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import javax.annotation.CheckForNull;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.DebtRemediationFunction.Type;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.issue.DefaultIssue;

public class DebtCalculator {

  private final RuleRepository ruleRepository;
  private final Durations durations;

  public DebtCalculator(RuleRepository ruleRepository, Durations durations) {
    this.ruleRepository = ruleRepository;
    this.durations = durations;
  }

  @CheckForNull
  public Duration calculate(DefaultIssue issue) {
    Rule rule = ruleRepository.getByKey(issue.ruleKey());
    DebtRemediationFunction fn = rule.getRemediationFunction();
    if (fn != null) {
      verifyEffortToFix(issue, fn);

      Duration debt = Duration.create(0);
      String gapMultiplier =fn.gapMultiplier();
      if (fn.type().usesGapMultiplier() && !Strings.isNullOrEmpty(gapMultiplier)) {
        int effortToFixValue = MoreObjects.firstNonNull(issue.effortToFix(), 1).intValue();
        // TODO convert to Duration directly in Rule#remediationFunction -> better performance + error handling
        debt = durations.decode(gapMultiplier).multiply(effortToFixValue);
      }
      String baseEffort= fn.baseEffort();
      if (fn.type().usesBaseEffort() && !Strings.isNullOrEmpty(baseEffort)) {
        // TODO convert to Duration directly in Rule#remediationFunction -> better performance + error handling
        debt = debt.add(durations.decode(baseEffort));
      }
      return debt;
    }
    return null;
  }

  private static void verifyEffortToFix(DefaultIssue issue, DebtRemediationFunction fn) {
    if (Type.CONSTANT_ISSUE.equals(fn.type()) && issue.effortToFix() != null) {
      throw new IllegalArgumentException("Rule '" + issue.getRuleKey() + "' can not use 'Constant/issue' remediation function " +
        "because this rule does not have a fixed remediation cost.");
    }
  }
}

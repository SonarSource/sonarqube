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
package org.sonar.server.measure.live;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;

import static java.util.Arrays.asList;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.RATING_BY_SEVERITY;

public class IssueMetricFormulaFactoryImpl implements IssueMetricFormulaFactory {

  private static final List<IssueMetricFormula> FORMULAS = asList(
    new IssueMetricFormula(CoreMetrics.CODE_SMELLS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.CODE_SMELL, false))),

    new IssueMetricFormula(CoreMetrics.BUGS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.BUG, false))),

    new IssueMetricFormula(CoreMetrics.VULNERABILITIES, false,
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.VULNERABILITY, false))),

    new IssueMetricFormula(CoreMetrics.VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolved(false))),

    new IssueMetricFormula(CoreMetrics.BLOCKER_VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.BLOCKER, false))),

    new IssueMetricFormula(CoreMetrics.CRITICAL_VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.CRITICAL, false))),

    new IssueMetricFormula(CoreMetrics.MAJOR_VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MAJOR, false))),

    new IssueMetricFormula(CoreMetrics.MINOR_VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MINOR, false))),

    new IssueMetricFormula(CoreMetrics.INFO_VIOLATIONS, false,
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.INFO, false))),

    new IssueMetricFormula(CoreMetrics.FALSE_POSITIVE_ISSUES, false,
      (context, issues) -> context.setValue(issues.countByResolution(Issue.RESOLUTION_FALSE_POSITIVE, false))),

    new IssueMetricFormula(CoreMetrics.WONT_FIX_ISSUES, false,
      (context, issues) -> context.setValue(issues.countByResolution(Issue.RESOLUTION_WONT_FIX, false))),

    new IssueMetricFormula(CoreMetrics.OPEN_ISSUES, false,
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_OPEN, false))),

    new IssueMetricFormula(CoreMetrics.REOPENED_ISSUES, false,
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_REOPENED, false))),

    new IssueMetricFormula(CoreMetrics.CONFIRMED_ISSUES, false,
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_CONFIRMED, false))),

    new IssueMetricFormula(CoreMetrics.TECHNICAL_DEBT, false,
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.CODE_SMELL, false))),

    new IssueMetricFormula(CoreMetrics.RELIABILITY_REMEDIATION_EFFORT, false,
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.BUG, false))),

    new IssueMetricFormula(CoreMetrics.SECURITY_REMEDIATION_EFFORT, false,
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.VULNERABILITY, false))),

    new IssueMetricFormula(CoreMetrics.SQALE_DEBT_RATIO, false,
      (context, issues) -> context.setValue(100.0 * debtDensity(context)),
      asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new IssueMetricFormula(CoreMetrics.SQALE_RATING, false,
      (context, issues) -> context
        .setValue(context.getDebtRatingGrid().getRatingForDensity(debtDensity(context))),
      asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new IssueMetricFormula(CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A, false,
      (context, issues) -> context.setValue(effortToReachMaintainabilityRatingA(context)), asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new IssueMetricFormula(CoreMetrics.RELIABILITY_RATING, false,
      (context, issues) -> context.setValue(RATING_BY_SEVERITY.get(issues.getHighestSeverityOfUnresolved(RuleType.BUG, false).orElse(Severity.INFO)))),

    new IssueMetricFormula(CoreMetrics.SECURITY_RATING, false,
      (context, issues) -> context.setValue(RATING_BY_SEVERITY.get(issues.getHighestSeverityOfUnresolved(RuleType.VULNERABILITY, false).orElse(Severity.INFO)))),

    new IssueMetricFormula(CoreMetrics.NEW_CODE_SMELLS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedByType(RuleType.CODE_SMELL, true))),

    new IssueMetricFormula(CoreMetrics.NEW_BUGS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedByType(RuleType.BUG, true))),

    new IssueMetricFormula(CoreMetrics.NEW_VULNERABILITIES, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedByType(RuleType.VULNERABILITY, true))),

    new IssueMetricFormula(CoreMetrics.NEW_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolved(true))),

    new IssueMetricFormula(CoreMetrics.NEW_BLOCKER_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedBySeverity(Severity.BLOCKER, true))),

    new IssueMetricFormula(CoreMetrics.NEW_CRITICAL_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedBySeverity(Severity.CRITICAL, true))),

    new IssueMetricFormula(CoreMetrics.NEW_MAJOR_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedBySeverity(Severity.MAJOR, true))),

    new IssueMetricFormula(CoreMetrics.NEW_MINOR_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedBySeverity(Severity.MINOR, true))),

    new IssueMetricFormula(CoreMetrics.NEW_INFO_VIOLATIONS, true,
      (context, issues) -> context.setLeakValue(issues.countUnresolvedBySeverity(Severity.INFO, true))),

    new IssueMetricFormula(CoreMetrics.NEW_TECHNICAL_DEBT, true,
      (context, issues) -> context.setLeakValue(issues.sumEffortOfUnresolved(RuleType.CODE_SMELL, true))),

    new IssueMetricFormula(CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT, true,
      (context, issues) -> context.setLeakValue(issues.sumEffortOfUnresolved(RuleType.BUG, true))),

    new IssueMetricFormula(CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT, true,
      (context, issues) -> context.setLeakValue(issues.sumEffortOfUnresolved(RuleType.VULNERABILITY, true))),

    new IssueMetricFormula(CoreMetrics.NEW_RELIABILITY_RATING, true,
      (context, issues) -> {
        String highestSeverity = issues.getHighestSeverityOfUnresolved(RuleType.BUG, true).orElse(Severity.INFO);
        context.setLeakValue(RATING_BY_SEVERITY.get(highestSeverity));
      }),

    new IssueMetricFormula(CoreMetrics.NEW_SECURITY_RATING, true,
      (context, issues) -> {
        String highestSeverity = issues.getHighestSeverityOfUnresolved(RuleType.VULNERABILITY, true).orElse(Severity.INFO);
        context.setLeakValue(RATING_BY_SEVERITY.get(highestSeverity));
      }),

    new IssueMetricFormula(CoreMetrics.NEW_SQALE_DEBT_RATIO, true,
      (context, issues) -> context.setLeakValue(100.0 * newDebtDensity(context)),
      asList(CoreMetrics.NEW_TECHNICAL_DEBT, CoreMetrics.NEW_DEVELOPMENT_COST)),

    new IssueMetricFormula(CoreMetrics.NEW_MAINTAINABILITY_RATING, true,
      (context, issues) -> context.setLeakValue(context.getDebtRatingGrid().getRatingForDensity(
        newDebtDensity(context))),
      asList(CoreMetrics.NEW_TECHNICAL_DEBT, CoreMetrics.NEW_DEVELOPMENT_COST)));

  private static final Set<Metric> FORMULA_METRICS = IssueMetricFormulaFactory.extractMetrics(FORMULAS);

  private static double debtDensity(IssueMetricFormula.Context context) {
    double debt = Math.max(context.getValue(CoreMetrics.TECHNICAL_DEBT).orElse(0.0), 0.0);
    Optional<Double> devCost = context.getValue(CoreMetrics.DEVELOPMENT_COST);
    if (devCost.isPresent() && Double.doubleToRawLongBits(devCost.get()) > 0L) {
      return debt / devCost.get();
    }
    return 0d;
  }

  private static double newDebtDensity(IssueMetricFormula.Context context) {
    double debt = Math.max(context.getLeakValue(CoreMetrics.NEW_TECHNICAL_DEBT).orElse(0.0), 0.0);
    Optional<Double> devCost = context.getLeakValue(CoreMetrics.NEW_DEVELOPMENT_COST);
    if (devCost.isPresent() && Double.doubleToRawLongBits(devCost.get()) > 0L) {
      return debt / devCost.get();
    }
    return 0d;
  }

  private static double effortToReachMaintainabilityRatingA(IssueMetricFormula.Context context) {
    double developmentCost = context.getValue(CoreMetrics.DEVELOPMENT_COST).orElse(0.0);
    double effort = context.getValue(CoreMetrics.TECHNICAL_DEBT).orElse(0.0);
    double upperGradeCost = context.getDebtRatingGrid().getGradeLowerBound(Rating.B) * developmentCost;
    return upperGradeCost < effort ? (effort - upperGradeCost) : 0.0;
  }

  @Override
  public List<IssueMetricFormula> getFormulas() {
    return FORMULAS;
  }

  @Override
  public Set<Metric> getFormulaMetrics() {
    return FORMULA_METRICS;
  }
}

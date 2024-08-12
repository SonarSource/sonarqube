/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.server.measure.ImpactMeasureBuilder;
import org.sonar.server.measure.Rating;

import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REVIEW_RATING;
import static org.sonar.server.measure.Rating.RATING_BY_SEVERITY;
import static org.sonar.server.measure.Rating.RATING_BY_SOFTWARE_QUALITY_SEVERITY;
import static org.sonar.server.metric.IssueCountMetrics.PRIORITIZED_RULE_ISSUES;
import static org.sonar.server.security.SecurityReviewRating.computeAToDRating;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

public class MeasureUpdateFormulaFactoryImpl implements MeasureUpdateFormulaFactory {
  private static final List<MeasureUpdateFormula> FORMULAS = asList(
    new MeasureUpdateFormula(CODE_SMELLS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.CODE_SMELL, false))),

    new MeasureUpdateFormula(CoreMetrics.BUGS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.BUG, false))),

    new MeasureUpdateFormula(CoreMetrics.VULNERABILITIES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.VULNERABILITY, false))),

    new MeasureUpdateFormula(PRIORITIZED_RULE_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countPrioritizedRuleIssues())),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_HOTSPOTS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.SECURITY_HOTSPOT, false))),

    new MeasureUpdateFormula(CoreMetrics.RELIABILITY_ISSUES, false, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.RELIABILITY, false))),

    new MeasureUpdateFormula(CoreMetrics.MAINTAINABILITY_ISSUES, false, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.MAINTAINABILITY, false))),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_ISSUES, false, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.SECURITY, false))),

    new MeasureUpdateFormula(CoreMetrics.NEW_RELIABILITY_ISSUES, true, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.RELIABILITY, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_MAINTAINABILITY_ISSUES, true, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.MAINTAINABILITY, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_SECURITY_ISSUES, true, true, new ImpactAddChildren(),
      (context, issues) -> context.setValue(issues.getBySoftwareQuality(SoftwareQuality.SECURITY, true))),

    new MeasureUpdateFormula(CoreMetrics.VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolved(false))),

    new MeasureUpdateFormula(CoreMetrics.BLOCKER_VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.BLOCKER, false))),

    new MeasureUpdateFormula(CoreMetrics.CRITICAL_VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.CRITICAL, false))),

    new MeasureUpdateFormula(CoreMetrics.MAJOR_VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MAJOR, false))),

    new MeasureUpdateFormula(CoreMetrics.MINOR_VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MINOR, false))),

    new MeasureUpdateFormula(CoreMetrics.INFO_VIOLATIONS, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.INFO, false))),

    new MeasureUpdateFormula(CoreMetrics.FALSE_POSITIVE_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByResolution(Issue.RESOLUTION_FALSE_POSITIVE, false))),

    new MeasureUpdateFormula(CoreMetrics.ACCEPTED_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByResolution(Issue.RESOLUTION_WONT_FIX, false))),

    new MeasureUpdateFormula(CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES, false, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countHighImpactAccepted(false))),

    new MeasureUpdateFormula(CoreMetrics.OPEN_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_OPEN, false))),

    new MeasureUpdateFormula(CoreMetrics.REOPENED_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_REOPENED, false))),

    new MeasureUpdateFormula(CoreMetrics.CONFIRMED_ISSUES, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByStatus(Issue.STATUS_CONFIRMED, false))),

    new MeasureUpdateFormula(CoreMetrics.TECHNICAL_DEBT, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.CODE_SMELL, false))),

    new MeasureUpdateFormula(CoreMetrics.RELIABILITY_REMEDIATION_EFFORT, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.BUG, false))),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_REMEDIATION_EFFORT, false, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.VULNERABILITY, false))),

    new MeasureUpdateFormula(CoreMetrics.SQALE_DEBT_RATIO, false, false,
      (context, formula) -> context.setValue(100.0 * debtDensity(TECHNICAL_DEBT, context)),
      (context, issues) -> context.setValue(100.0 * debtDensity(TECHNICAL_DEBT, context)),
      asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(CoreMetrics.SQALE_RATING, false, false,
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getRatingForDensity(debtDensity(TECHNICAL_DEBT, context))),
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getRatingForDensity(debtDensity(TECHNICAL_DEBT, context))),
      asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A, false, false,
      (context, formula) -> context.setValue(effortToReachMaintainabilityRatingA(CoreMetrics.TECHNICAL_DEBT, context)),
      (context, issues) -> context.setValue(effortToReachMaintainabilityRatingA(CoreMetrics.TECHNICAL_DEBT, context)),
      asList(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(CoreMetrics.RELIABILITY_RATING, false, new MaxRatingChildren(),
      (context, issues) -> context.setValue(RATING_BY_SEVERITY.get(issues.getHighestSeverityOfUnresolved(RuleType.BUG, false).orElse(Severity.INFO)))),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_RATING, false, new MaxRatingChildren(),
      (context, issues) -> context.setValue(RATING_BY_SEVERITY.get(issues.getHighestSeverityOfUnresolved(RuleType.VULNERABILITY, false).orElse(Severity.INFO)))),

    new MeasureUpdateFormula(SECURITY_HOTSPOTS_REVIEWED_STATUS, false,
      (context, formula) -> context.setValue(context.getValue(SECURITY_HOTSPOTS_REVIEWED_STATUS).orElse(0D) + context.getChildrenHotspotsReviewed()),
      (context, issues) -> context.setValue(issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, false))),

    new MeasureUpdateFormula(SECURITY_HOTSPOTS_TO_REVIEW_STATUS, false,
      (context, formula) -> context.setValue(context.getValue(SECURITY_HOTSPOTS_TO_REVIEW_STATUS).orElse(0D) + context.getChildrenHotspotsToReview()),
      (context, issues) -> context.setValue(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, false))),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_HOTSPOTS_REVIEWED, false,
      (context, formula) -> {
        Optional<Double> percent = computePercent(
          context.getValue(SECURITY_HOTSPOTS_TO_REVIEW_STATUS).orElse(0D).longValue(),
          context.getValue(SECURITY_HOTSPOTS_REVIEWED_STATUS).orElse(0D).longValue());
        percent.ifPresent(context::setValue);
      },
      (context, issues) -> computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, false), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, false))
        .ifPresent(context::setValue)),

    new MeasureUpdateFormula(CoreMetrics.SECURITY_REVIEW_RATING, false,
      (context, formula) -> context.setValue(computeRating(context.getValue(SECURITY_HOTSPOTS_REVIEWED).orElse(null))),
      (context, issues) -> {
        Optional<Double> percent = computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, false), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, false));
        context.setValue(computeRating(percent.orElse(null)));
      }),

    new MeasureUpdateFormula(CoreMetrics.NEW_CODE_SMELLS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.CODE_SMELL, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_BUGS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.BUG, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_VULNERABILITIES, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.VULNERABILITY, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_SECURITY_HOTSPOTS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedByType(RuleType.SECURITY_HOTSPOT, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolved(true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_BLOCKER_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.BLOCKER, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_CRITICAL_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.CRITICAL, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_MAJOR_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MAJOR, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_MINOR_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.MINOR, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_INFO_VIOLATIONS, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countUnresolvedBySeverity(Severity.INFO, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_ACCEPTED_ISSUES, true, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.countByResolution(Issue.RESOLUTION_WONT_FIX, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_TECHNICAL_DEBT, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.CODE_SMELL, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.BUG, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolved(RuleType.VULNERABILITY, true))),

    new MeasureUpdateFormula(CoreMetrics.NEW_RELIABILITY_RATING, true, new MaxRatingChildren(),
      (context, issues) -> {
        String highestSeverity = issues.getHighestSeverityOfUnresolved(RuleType.BUG, true).orElse(Severity.INFO);
        context.setValue(RATING_BY_SEVERITY.get(highestSeverity));
      }),

    new MeasureUpdateFormula(CoreMetrics.NEW_SECURITY_RATING, true, new MaxRatingChildren(),
      (context, issues) -> {
        String highestSeverity = issues.getHighestSeverityOfUnresolved(RuleType.VULNERABILITY, true).orElse(Severity.INFO);
        context.setValue(RATING_BY_SEVERITY.get(highestSeverity));
      }),

    new MeasureUpdateFormula(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS, true,
      (context, formula) -> context.setValue(context.getValue(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS).orElse(0D) + context.getChildrenNewHotspotsReviewed()),
      (context, issues) -> context.setValue(issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, true))),

    new MeasureUpdateFormula(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS, true,
      (context, formula) -> context.setValue(context.getValue(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS).orElse(0D) + context.getChildrenNewHotspotsToReview()),
      (context, issues) -> context.setValue(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, true))),

    new MeasureUpdateFormula(NEW_SECURITY_HOTSPOTS_REVIEWED, true,
      (context, formula) -> {
        Optional<Double> percent = computePercent(
          context.getValue(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS).orElse(0D).longValue(),
          context.getValue(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS).orElse(0D).longValue());
        percent.ifPresent(context::setValue);
      },
      (context, issues) -> computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, true), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, true))
        .ifPresent(context::setValue)),

    new MeasureUpdateFormula(CoreMetrics.NEW_SECURITY_REVIEW_RATING, true,
      (context, formula) -> context.setValue(computeRating(context.getValue(NEW_SECURITY_HOTSPOTS_REVIEWED).orElse(null))),
      (context, issues) -> {
        Optional<Double> percent = computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, true), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, true));
        context.setValue(computeRating(percent.orElse(null)));
      }),

    new MeasureUpdateFormula(CoreMetrics.NEW_SQALE_DEBT_RATIO, true, false,
      (context, formula) -> context.setValue(100.0D * newDebtDensity(CoreMetrics.NEW_TECHNICAL_DEBT, context)),
      (context, issues) -> context.setValue(100.0D * newDebtDensity(CoreMetrics.NEW_TECHNICAL_DEBT, context)),
      asList(CoreMetrics.NEW_TECHNICAL_DEBT, CoreMetrics.NEW_DEVELOPMENT_COST)),

    new MeasureUpdateFormula(CoreMetrics.NEW_MAINTAINABILITY_RATING, true, false,
      (context, formula) -> context.setValue(context.getDebtRatingGrid().getRatingForDensity(newDebtDensity(CoreMetrics.NEW_TECHNICAL_DEBT, context))),
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getRatingForDensity(newDebtDensity(CoreMetrics.NEW_TECHNICAL_DEBT, context))),
      asList(CoreMetrics.NEW_TECHNICAL_DEBT, CoreMetrics.NEW_DEVELOPMENT_COST)),

    // Metrics based on Software Qualities
    new MeasureUpdateFormula(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, false, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.MAINTAINABILITY, false))),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT, false, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.RELIABILITY, false))),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT, false, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.SECURITY, false))),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, true, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.MAINTAINABILITY, true))),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT, true, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.RELIABILITY, true))),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT, true, true, new AddChildren(),
      (context, issues) -> context.setValue(issues.sumEffortOfUnresolvedBySoftwareQuality(SoftwareQuality.SECURITY, true))),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO, false, true,
      (context, formula) -> context.setValue(100.0 * debtDensity(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      (context, issues) -> context.setValue(100.0 * debtDensity(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      asList(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO, true, true,
      (context, formula) -> context.setValue(100.0D * newDebtDensity(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      (context, issues) -> context.setValue(100.0D * newDebtDensity(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      asList(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, CoreMetrics.NEW_DEVELOPMENT_COST)),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_MAINTAINABILITY_RATING, false, true,
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getAToDRatingForDensity(debtDensity(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context))),
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getAToDRatingForDensity(debtDensity(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context))),
      asList(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING, true, true,
      (context, formula) -> context.setValue(context.getDebtRatingGrid().getAToDRatingForDensity(newDebtDensity(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context))),
      (context, issues) -> context.setValue(context.getDebtRatingGrid().getAToDRatingForDensity(newDebtDensity(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context))),
      asList(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, CoreMetrics.NEW_DEVELOPMENT_COST)),

    new MeasureUpdateFormula(EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A, false, true,
      (context, formula) -> context.setValue(effortToReachMaintainabilityRatingA(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      (context, issues) -> context.setValue(effortToReachMaintainabilityRatingA(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, context)),
      asList(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT, CoreMetrics.DEVELOPMENT_COST)),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_RELIABILITY_RATING, false, true, new MaxRatingChildren(),
      (context, issues) -> {
        Rating rating = issues.getHighestSeverityOfUnresolved(SoftwareQuality.RELIABILITY, false)
          .map(RATING_BY_SOFTWARE_QUALITY_SEVERITY::get)
          .orElse(Rating.A);
        context.setValue(rating);
      }),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING, true, true, new MaxRatingChildren(),
      (context, issues) -> {
        Rating rating = issues.getHighestSeverityOfUnresolved(SoftwareQuality.RELIABILITY, true)
          .map(RATING_BY_SOFTWARE_QUALITY_SEVERITY::get)
          .orElse(Rating.A);
        context.setValue(rating);
      }),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_SECURITY_RATING, false, true, new MaxRatingChildren(),
      (context, issues) -> {
        Rating rating = issues.getHighestSeverityOfUnresolved(SoftwareQuality.SECURITY, false)
          .map(RATING_BY_SOFTWARE_QUALITY_SEVERITY::get)
          .orElse(Rating.A);
        context.setValue(rating);
      }),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_SECURITY_RATING, true, true, new MaxRatingChildren(),
      (context, issues) -> {
        Rating rating = issues.getHighestSeverityOfUnresolved(SoftwareQuality.SECURITY, true)
          .map(RATING_BY_SOFTWARE_QUALITY_SEVERITY::get)
          .orElse(Rating.A);
        context.setValue(rating);
      }),

    new MeasureUpdateFormula(SOFTWARE_QUALITY_SECURITY_REVIEW_RATING, false, true,
      (context, formula) -> context.setValue(computeAToDRating(context.getValue(SECURITY_HOTSPOTS_REVIEWED).orElse(null))),
      (context, issues) -> {
        Optional<Double> percent = computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, false), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, false));
        context.setValue(computeAToDRating(percent.orElse(null)));
      }),

    new MeasureUpdateFormula(NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING, true, true,
      (context, formula) -> context.setValue(computeAToDRating(context.getValue(NEW_SECURITY_HOTSPOTS_REVIEWED).orElse(null))),
      (context, issues) -> {
        Optional<Double> percent = computePercent(issues.countHotspotsByStatus(Issue.STATUS_TO_REVIEW, true), issues.countHotspotsByStatus(Issue.STATUS_REVIEWED, true));
        context.setValue(computeAToDRating(percent.orElse(null)));
      }));

  private static final Set<Metric> FORMULA_METRICS = MeasureUpdateFormulaFactory.extractMetrics(FORMULAS);

  private static double debtDensity(Metric<?> maintainabilityRemediationEffortMetric, MeasureUpdateFormula.Context context) {
    double debt = Math.max(context.getValue(maintainabilityRemediationEffortMetric).orElse(0.0D), 0.0D);
    Optional<Double> devCost = context.getText(CoreMetrics.DEVELOPMENT_COST).map(Double::parseDouble);
    if (devCost.isPresent() && Double.doubleToRawLongBits(devCost.get()) > 0L) {
      return debt / devCost.get();
    }
    return 0.0D;
  }

  private static double newDebtDensity(Metric<?> maintainabilityRemediationEffortMetric, MeasureUpdateFormula.Context context) {
    double debt = Math.max(context.getValue(maintainabilityRemediationEffortMetric).orElse(0.0D), 0.0D);
    Optional<Double> devCost = context.getValue(CoreMetrics.NEW_DEVELOPMENT_COST);
    if (devCost.isPresent() && Double.doubleToRawLongBits(devCost.get()) > 0L) {
      return debt / devCost.get();
    }
    return 0.0D;
  }

  private static double effortToReachMaintainabilityRatingA(Metric<?> maintainabilityRemediationEffortMetric, MeasureUpdateFormula.Context context) {
    double developmentCost = context.getText(CoreMetrics.DEVELOPMENT_COST).map(Double::parseDouble).orElse(0.0D);
    double effort = context.getValue(maintainabilityRemediationEffortMetric).orElse(0.0D);
    double upperGradeCost = context.getDebtRatingGrid().getGradeLowerBound(Rating.B) * developmentCost;
    return upperGradeCost < effort ? (effort - upperGradeCost) : 0.0D;
  }

  static class AddChildren implements BiConsumer<MeasureUpdateFormula.Context, MeasureUpdateFormula> {
    @Override
    public void accept(MeasureUpdateFormula.Context context, MeasureUpdateFormula formula) {
      double sum = context.getChildrenValues().stream().mapToDouble(x -> x).sum();
      context.setValue(context.getValue(formula.getMetric()).orElse(0D) + sum);
    }
  }

  private static class MaxRatingChildren implements BiConsumer<MeasureUpdateFormula.Context, MeasureUpdateFormula> {
    @Override
    public void accept(MeasureUpdateFormula.Context context, MeasureUpdateFormula formula) {
      OptionalInt max = context.getChildrenValues().stream().mapToInt(Double::intValue).max();
      if (max.isPresent()) {
        int currentRating = context.getValue(formula.getMetric()).map(Double::intValue).orElse(Rating.A.getIndex());
        context.setValue(Rating.valueOf(Math.max(currentRating, max.getAsInt())));
      }
    }
  }

  private static class ImpactAddChildren implements BiConsumer<MeasureUpdateFormula.Context, MeasureUpdateFormula> {
    @Override
    public void accept(MeasureUpdateFormula.Context context, MeasureUpdateFormula formula) {
      ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.createEmpty();
      context.getChildrenTextValues().stream()
        .map(ImpactMeasureBuilder::fromString)
        .forEach(impactMeasureBuilder::add);
      context.getText(formula.getMetric()).ifPresent(value -> impactMeasureBuilder.add(ImpactMeasureBuilder.fromString(value)));
      context.setValue(impactMeasureBuilder.buildAsString());
    }
  }

  @Override
  public List<MeasureUpdateFormula> getFormulas() {
    return FORMULAS;
  }

  @Override
  public Set<Metric> getFormulaMetrics() {
    return FORMULA_METRICS;
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue.commonrule;

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;

public abstract class AbstractCoverageRule extends CommonRule {

  private final MeasureRepository measureRepository;
  private final Metric coverageMetric;
  private final Metric uncoveredMetric;
  private final Metric toCoverMetric;
  private final String minPropertyKey;

  public AbstractCoverageRule(ActiveRulesHolder activeRulesHolder, String ruleKey, String minPropertyKey,
    MeasureRepository measureRepository,
    Metric coverageMetric, Metric uncoveredMetric, Metric toCoverMetric) {
    super(activeRulesHolder, ruleKey);
    this.minPropertyKey = minPropertyKey;
    this.measureRepository = measureRepository;
    this.coverageMetric = coverageMetric;
    this.uncoveredMetric = uncoveredMetric;
    this.toCoverMetric = toCoverMetric;
  }

  @Override
  protected CommonRuleIssue doProcessFile(Component file, ActiveRule activeRule) {
    Optional<Measure> coverageMeasure = measureRepository.getRawMeasure(file, coverageMetric);
    if (!file.getFileAttributes().isUnitTest() && coverageMeasure.isPresent()) {
      double coverage = coverageMeasure.get().getDoubleValue();
      double minimumCoverage = getMinDensityParam(activeRule, minPropertyKey);
      if (coverage < minimumCoverage) {
        return generateIssue(file, minimumCoverage);
      }
    }
    return null;
  }

  private CommonRuleIssue generateIssue(Component file, double minimumCoverage) {
    Optional<Measure> uncoveredMeasure = measureRepository.getRawMeasure(file, uncoveredMetric);
    Optional<Measure> toCoverMeasure = measureRepository.getRawMeasure(file, toCoverMetric);
    double uncovered = uncoveredMeasure.isPresent() ? uncoveredMeasure.get().getIntValue() : 0.0;
    double toCover = toCoverMeasure.isPresent() ? toCoverMeasure.get().getIntValue() : 0.0;

    // effort to fix is the number of lines/conditions to cover for reaching threshold
    int effortToFix = (int) Math.ceil((toCover * minimumCoverage / 100) - (toCover - uncovered));

    return new CommonRuleIssue(effortToFix, formatMessage(effortToFix, minimumCoverage));
  }

  protected abstract String formatMessage(int effortToFix, double minCoverage);
}

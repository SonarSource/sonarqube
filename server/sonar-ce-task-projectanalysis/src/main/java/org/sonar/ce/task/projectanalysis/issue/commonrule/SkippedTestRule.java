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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.server.rule.CommonRuleKeys;

import static java.lang.String.format;

public class SkippedTestRule extends CommonRule {

  private final MeasureRepository measureRepository;
  private final Metric skippedTestsMetric;

  public SkippedTestRule(ActiveRulesHolder activeRulesHolder, MeasureRepository measureRepository, MetricRepository metricRepository) {
    super(activeRulesHolder, CommonRuleKeys.SKIPPED_UNIT_TESTS);
    this.measureRepository = measureRepository;
    this.skippedTestsMetric = metricRepository.getByKey(CoreMetrics.SKIPPED_TESTS_KEY);
  }

  @Override
  protected CommonRuleIssue doProcessFile(Component file, ActiveRule activeRule) {
    Optional<Measure> measure = measureRepository.getRawMeasure(file, skippedTestsMetric);

    int skipped = measure.map(Measure::getIntValue).orElse(0);
    if (skipped > 0) {
      String message = format("Fix or remove skipped unit tests in file \"%s\".", file.getName());
      return new CommonRuleIssue(skipped, message);
    }
    return null;
  }
}

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
package org.sonar.server.computation.task.projectanalysis.issue.commonrule;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolder;
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

    int skipped = measure.isPresent() ? measure.get().getIntValue() : 0;
    if (skipped > 0) {
      String message = format("Fix or remove skipped unit tests in file \"%s\".", file.getName());
      return new CommonRuleIssue(skipped, message);
    }
    return null;
  }
}

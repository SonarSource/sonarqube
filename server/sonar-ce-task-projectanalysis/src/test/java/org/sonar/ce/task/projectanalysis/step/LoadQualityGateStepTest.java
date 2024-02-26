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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateServiceImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadQualityGateStepTest {
  @RegisterExtension
  private final MutableQualityGateHolderRule mutableQualityGateHolder = new MutableQualityGateHolderRule();

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private final QualityGateServiceImpl qualityGateService = mock(QualityGateServiceImpl.class);

  private final LoadQualityGateStep underTest = new LoadQualityGateStep(qualityGateService, mutableQualityGateHolder, analysisMetadataHolder);
  private final Project project = mock(Project.class);

  @BeforeEach
  void before() {
    when(analysisMetadataHolder.getProject()).thenReturn(project);
  }

  @Test
  void filter_conditions_on_pull_request() {
    Metric newMetric = new MetricImpl("1", "new_key", "name", Metric.MetricType.INT);
    Metric metric = new MetricImpl("2", "key", "name", Metric.MetricType.INT);
    Condition variation = new Condition(newMetric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");
    Condition condition = new Condition(metric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");

    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    QualityGate defaultGate = new QualityGate("1", "qg", Arrays.asList(variation, condition));
    when(qualityGateService.findEffectiveQualityGate(project)).thenReturn(defaultGate);

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate().get().getConditions()).containsExactly(variation);
  }

  @Test
  void execute_sets_effective_quality_gate() {
    QualityGate qg = mock(QualityGate.class);
    when(qualityGateService.findEffectiveQualityGate(project)).thenReturn(qg);

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate()).containsSame(qg);
  }
}

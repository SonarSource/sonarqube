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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadQualityGateStepTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public MutableQualityGateHolderRule mutableQualityGateHolder = new MutableQualityGateHolderRule();

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private QualityGateService qualityGateService = mock(QualityGateService.class);

  private LoadQualityGateStep underTest = new LoadQualityGateStep(qualityGateService, mutableQualityGateHolder, analysisMetadataHolder);

  @Before
  public void setUp() {
    when(analysisMetadataHolder.isShortLivingBranch()).thenReturn(false);
    when(analysisMetadataHolder.getOrganization()).thenReturn(mock(Organization.class));
  }

  @Test
  public void filter_conditions_on_short_living_branch() {

    Metric newMetric = new MetricImpl(1, "new_key", "name", Metric.MetricType.INT);
    Metric metric = new MetricImpl(2, "key", "name", Metric.MetricType.INT);
    Condition variation = new Condition(newMetric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");
    Condition condition = new Condition(metric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");

    when(analysisMetadataHolder.isSLBorPR()).thenReturn(true);
    QualityGate defaultGate = new QualityGate(1, "qg", Arrays.asList(variation, condition));
    when(qualityGateService.findDefaultQualityGate(any(Organization.class))).thenReturn(defaultGate);

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate().get().getConditions()).containsExactly(variation);
  }

  @Test
  public void filter_conditions_on_pull_request() {
    Metric newMetric = new MetricImpl(1, "new_key", "name", Metric.MetricType.INT);
    Metric metric = new MetricImpl(2, "key", "name", Metric.MetricType.INT);
    Condition variation = new Condition(newMetric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");
    Condition condition = new Condition(metric, Condition.Operator.GREATER_THAN.getDbValue(), "1.0");

    when(analysisMetadataHolder.isSLBorPR()).thenReturn(true);
    QualityGate defaultGate = new QualityGate(1, "qg", Arrays.asList(variation, condition));
    when(qualityGateService.findDefaultQualityGate(any(Organization.class))).thenReturn(defaultGate);

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate().get().getConditions()).containsExactly(variation);
  }

  @Test
  public void execute_sets_default_QualityGate_when_project_has_no_settings() {
    QualityGate defaultGate = mock(QualityGate.class);
    when(qualityGateService.findDefaultQualityGate(any(Organization.class))).thenReturn(defaultGate);

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(defaultGate);
  }

  @Test
  public void execute_sets_QualityGate_if_it_can_be_found_by_service() {
    QualityGate qualityGate = new QualityGate(10, "name", emptyList());

    when(analysisMetadataHolder.getProject()).thenReturn(mock(Project.class));
    when(qualityGateService.findQualityGate(any(Project.class))).thenReturn(Optional.of(qualityGate));

    underTest.execute(new TestComputationStepContext());

    assertThat(mutableQualityGateHolder.getQualityGate().get()).isSameAs(qualityGate);
  }

}

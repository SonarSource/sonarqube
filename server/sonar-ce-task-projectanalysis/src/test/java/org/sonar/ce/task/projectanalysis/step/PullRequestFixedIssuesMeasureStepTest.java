/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureAssert;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PullRequestFixedIssuesMeasureStepTest {

  private static final int ROOT_REF = 1;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  private final PullRequestFixedIssueRepository pullRequestFixedIssueRepository = mock(PullRequestFixedIssueRepository.class);
  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  private final PullRequestFixedIssuesMeasureStep underTest = new PullRequestFixedIssuesMeasureStep(treeRootHolder, metricRepository,
    measureRepository, pullRequestFixedIssueRepository, analysisMetadataHolder);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, ROOT_REF).build());
    metricRepository.add(CoreMetrics.PULL_REQUEST_FIXED_ISSUES);
  }

  @Test
  public void execute_whenComponentIsPullRequest_shouldCreateMeasure() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(pullRequestFixedIssueRepository.getFixedIssues()).thenReturn(List.of(new DefaultIssue(), new DefaultIssue()));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).hasSize(1);
    Optional<Measure> addedRawMeasure = measureRepository.getAddedRawMeasure(ROOT_REF, CoreMetrics.PULL_REQUEST_FIXED_ISSUES_KEY);
    MeasureAssert.assertThat(addedRawMeasure).hasValue(2);
  }

  @Test
  public void execute_whenComponentIsNotPullRequest_shouldNotCreateMeasure() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).isEmpty();
  }

  @Test
  public void execute_whenNoFixedIssues_shouldCreateMeasureWithValueZero() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(pullRequestFixedIssueRepository.getFixedIssues()).thenReturn(Collections.emptyList());

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(ROOT_REF)).hasSize(1);
    Optional<Measure> addedRawMeasure = measureRepository.getAddedRawMeasure(ROOT_REF, CoreMetrics.PULL_REQUEST_FIXED_ISSUES_KEY);
    MeasureAssert.assertThat(addedRawMeasure).hasValue(0);
  }
}

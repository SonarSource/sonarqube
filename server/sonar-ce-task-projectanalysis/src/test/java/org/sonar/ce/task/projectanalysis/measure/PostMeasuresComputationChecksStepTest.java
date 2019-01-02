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
package org.sonar.ce.task.projectanalysis.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.PostMeasuresComputationCheck.Context;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.DUMB_PROJECT;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class PostMeasuresComputationChecksStepTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(DUMB_PROJECT);
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(NCLOC);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void execute_extensions() {
    PostMeasuresComputationCheck check1 = mock(PostMeasuresComputationCheck.class);
    PostMeasuresComputationCheck check2 = mock(PostMeasuresComputationCheck.class);

    newStep(check1, check2).execute(new TestComputationStepContext());

    InOrder inOrder = inOrder(check1, check2);
    inOrder.verify(check1).onCheck(any(Context.class));
    inOrder.verify(check2).onCheck(any(Context.class));
  }

  @Test
  public void context_contains_project_uuid_from_analysis_metada_holder() {
    Project project = Project.from(newPrivateProjectDto(newOrganizationDto()));
    analysisMetadataHolder.setProject(project);
    PostMeasuresComputationCheck check = mock(PostMeasuresComputationCheck.class);

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getProjectUuid()).isEqualTo(project.getUuid());
  }

  @Test
  public void context_contains_ncloc_when_available() {
    PostMeasuresComputationCheck check = mock(PostMeasuresComputationCheck.class);
    measureRepository.addRawMeasure(DUMB_PROJECT.getReportAttributes().getRef(), CoreMetrics.NCLOC_KEY, Measure.newMeasureBuilder().create(10));

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getNcloc()).isEqualTo(10);
  }

  @Test
  public void ncloc_is_zero_in_context_when_not_available() {
    PostMeasuresComputationCheck check = mock(PostMeasuresComputationCheck.class);

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getNcloc()).isEqualTo(0);
  }

  @Test
  public void do_nothing_if_no_extensions() {
    // no failure
    newStep().execute(new TestComputationStepContext());
  }

  @Test
  public void fail_if_an_extension_throws_an_exception() {
    PostMeasuresComputationCheck check1 = mock(PostMeasuresComputationCheck.class);
    PostMeasuresComputationCheck check2 = mock(PostMeasuresComputationCheck.class);
    doThrow(new IllegalStateException("BOOM")).when(check2).onCheck(any(Context.class));
    PostMeasuresComputationCheck check3 = mock(PostMeasuresComputationCheck.class);

    try {
      newStep(check1, check2, check3).execute(new TestComputationStepContext());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("BOOM");
      verify(check1).onCheck(any(Context.class));
      verify(check3, never()).onCheck(any(Context.class));
    }
  }

  @Test
  public void test_getDescription() {
    assertThat(newStep().getDescription()).isNotEmpty();
  }

  private PostMeasuresComputationChecksStep newStep(PostMeasuresComputationCheck... postMeasuresComputationChecks) {
    if (postMeasuresComputationChecks.length == 0) {
      return new PostMeasuresComputationChecksStep(treeRootHolder, metricRepository, measureRepository, analysisMetadataHolder);
    }
    return new PostMeasuresComputationChecksStep(treeRootHolder, metricRepository, measureRepository, analysisMetadataHolder, postMeasuresComputationChecks);
  }
}

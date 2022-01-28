/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.ce.posttask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.ce.posttask.Branch.Type.BRANCH;

public class PostProjectAnalysisTaskTesterTest {

  private CeTask ceTask = mock(CeTask.class);
  private Project project = mock(Project.class);
  private long someDateAsLong = 846351351684351L;
  private Date someDate = new Date(someDateAsLong);
  private String analysisUuid = RandomStringUtils.randomAlphanumeric(40);
  private QualityGate qualityGate = mock(QualityGate.class);
  private CaptorPostProjectAnalysisTask captorPostProjectAnalysisTask = new CaptorPostProjectAnalysisTask();
  private PostProjectAnalysisTaskTester underTest = PostProjectAnalysisTaskTester.of(captorPostProjectAnalysisTask);

  @Test
  public void of_throws_NPE_if_PostProjectAnalysisTask_is_null() {
    assertThatThrownBy(() -> PostProjectAnalysisTaskTester.of(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("PostProjectAnalysisTask instance cannot be null");
  }

  @Test
  public void withCeTask_throws_NPE_if_ceTask_is_null() {
    assertThatThrownBy(() -> underTest.withCeTask(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ceTask cannot be null");
  }

  @Test
  public void withProject_throws_NPE_if_project_is_null() {
    assertThatThrownBy(() -> underTest.withProject(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("project cannot be null");
  }

  @Test
  public void at_throws_NPE_if_date_is_null() {
    assertThatThrownBy(() -> underTest.at(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("date cannot be null");
  }

  @Test
  public void withQualityGate_does_not_throw_NPE_if_project_is_null() {
    underTest.withQualityGate(null);
  }

  @Test
  public void execute_throws_NPE_if_ceTask_is_null() {
    underTest.withProject(project).at(someDate);

    assertThatThrownBy(() -> underTest.execute())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ceTask cannot be null");
  }

  @Test
  public void execute_throws_NPE_if_project_is_null() {
    underTest.withCeTask(ceTask).at(someDate);

    assertThatThrownBy(() -> underTest.execute())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("project cannot be null");
  }

  @Test
  public void verify_getters_of_ProjectAnalysis_object_passed_to_PostProjectAnalysisTask() {
    underTest.withCeTask(ceTask).withProject(project).withQualityGate(qualityGate).withAnalysisUuid(analysisUuid).at(someDate);

    underTest.execute();

    PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = captorPostProjectAnalysisTask.projectAnalysis;
    assertThat(projectAnalysis).isNotNull();
    assertThat(projectAnalysis.getCeTask()).isSameAs(ceTask);
    assertThat(projectAnalysis.getProject()).isSameAs(project);
    assertThat(projectAnalysis.getQualityGate()).isSameAs(qualityGate);
    assertThat(projectAnalysis.getAnalysis().get().getAnalysisUuid()).isSameAs(analysisUuid);
  }

  @Test
  public void verify_toString_of_ProjectAnalysis_object_passed_to_PostProjectAnalysisTask() {
    when(ceTask.toString()).thenReturn("CeTask");
    when(project.toString()).thenReturn("Project");
    when(qualityGate.toString()).thenReturn("QualityGate");
    underTest.withCeTask(ceTask).withProject(project).withQualityGate(qualityGate).at(someDate);

    underTest.execute();

    assertThat(captorPostProjectAnalysisTask.projectAnalysis)
      .hasToString("ProjectAnalysis{ceTask=CeTask, project=Project, date=846351351684351, analysisDate=846351351684351, qualityGate=QualityGate}");

  }

  @Test
  public void execute_throws_NPE_if_date_is_null() {
    underTest.withCeTask(ceTask).withProject(project);

    assertThatThrownBy(() -> underTest.execute())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("date cannot be null");
  }

  @Test
  public void getLogStatistics_throws_ISE_if_called_before_execute() {
    assertThatThrownBy(() -> underTest.getLogStatistics())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("execute must be called first");
  }

  @Test
  public void getLogStatistics_returns_empty_if_no_log_statistic_added_by_tested_Task() {
    underTest.withCeTask(ceTask).withProject(project).withQualityGate(qualityGate).withAnalysisUuid(analysisUuid).at(someDate);

    underTest.execute();

    assertThat(underTest.getLogStatistics()).isEmpty();
  }

  @Test
  public void getLogStatistics_returns_log_statistics_added_by_tested_Task() {
    Random random = new Random();
    Map<String, Object> expected = new HashMap<>();
    for (int i = 0; i < 1 + random.nextInt(10); i++) {
      expected.put(String.valueOf(i), random.nextInt(100));
    }
    PostProjectAnalysisTask projectAnalysisTask = mock(PostProjectAnalysisTask.class);
    doAnswer(i -> {
      PostProjectAnalysisTask.Context context = i.getArgument(0);
      expected.forEach((k, v) -> context.getLogStatistics().add(k, v));
      return null;
    }).when(projectAnalysisTask).finished(any(PostProjectAnalysisTask.Context.class));
    PostProjectAnalysisTaskTester underTest = PostProjectAnalysisTaskTester.of(projectAnalysisTask);
    underTest.withCeTask(ceTask).withProject(project).withQualityGate(qualityGate).withAnalysisUuid(analysisUuid).at(someDate);

    underTest.execute();

    assertThat(underTest.getLogStatistics()).isEqualTo(expected);
  }

  @Test
  public void branch_builder_builds_branch_of_type_branch_by_default() {
    Branch branch = PostProjectAnalysisTaskTester.newBranchBuilder().build();

    assertThat(branch.getType()).isEqualTo(BRANCH);
  }

  private static class CaptorPostProjectAnalysisTask implements PostProjectAnalysisTask {
    private ProjectAnalysis projectAnalysis;

    @Override
    public String getDescription() {
      return "captor";
    }

    @Override
    public void finished(Context context) {
      this.projectAnalysis = context.getProjectAnalysis();
    }
  }
}

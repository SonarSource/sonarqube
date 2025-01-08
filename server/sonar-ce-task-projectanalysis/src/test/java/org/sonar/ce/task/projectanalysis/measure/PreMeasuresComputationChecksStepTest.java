/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.measure.PreMeasuresComputationCheck.Context;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.measure.PreMeasuresComputationCheck.PreMeasuresComputationCheckException;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class PreMeasuresComputationChecksStepTest {

  public AnalysisMetadataHolderRule analysisMetadataHolder = mock();
  public CeTaskMessages ceTaskMessages = mock();
  public ConfigurationRepository configurationRepository = mock();

  @Before
  public void setup() {

  }

  @Test
  public void execute_extensions() throws PreMeasuresComputationCheckException {
    PreMeasuresComputationCheck check1 = mock(PreMeasuresComputationCheck.class);
    PreMeasuresComputationCheck check2 = mock(PreMeasuresComputationCheck.class);

    newStep(check1, check2).execute(new TestComputationStepContext());

    InOrder inOrder = inOrder(check1, check2);
    inOrder.verify(check1).onCheck(any(Context.class));
    inOrder.verify(check2).onCheck(any(Context.class));
  }

  @Test
  public void context_contains_project_uuid_from_analysis_metadata_holder() throws PreMeasuresComputationCheckException {
    Project project = Project.from(newPrivateProjectDto());
    when(analysisMetadataHolder.getProject()).thenReturn(project);
    PreMeasuresComputationCheck check = mock(PreMeasuresComputationCheck.class);

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getProjectUuid()).isEqualTo(project.getUuid());
  }

  @Test
  public void context_contains_pullRequest_key_from_analysis_metadata_holder() throws PreMeasuresComputationCheckException {
    mockPr("pr1");
    PreMeasuresComputationCheck check = mock(PreMeasuresComputationCheck.class);

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getBranch().getPullRequestKey()).isEqualTo("pr1");
  }

  @Test
  public void context_contains_branch_from_analysis_metadata_holder() throws PreMeasuresComputationCheckException {
    mockBranch("branchName");
    PreMeasuresComputationCheck check = mock(PreMeasuresComputationCheck.class);

    newStep(check).execute(new TestComputationStepContext());

    ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    verify(check).onCheck(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().getBranch().getName()).isEqualTo("branchName");
  }

  @Test
  public void whenCheckThrows_thenLogCeMessage() throws PreMeasuresComputationCheckException {
    PreMeasuresComputationCheck check = mock(PreMeasuresComputationCheck.class);
    doThrow(new PreMeasuresComputationCheckException("error"))
      .when(check).onCheck(any());

    newStep(check).execute(new TestComputationStepContext());

    var messageCaptor = ArgumentCaptor.forClass(CeTaskMessages.Message.class);
    verify(ceTaskMessages).add(messageCaptor.capture());
    assertThat(messageCaptor.getValue().getText()).isEqualTo("error");
  }

  @Test
  public void test_getDescription() {
    assertThat(newStep().getDescription()).isNotEmpty();
  }

  private PreMeasuresComputationChecksStep newStep(PreMeasuresComputationCheck... preMeasuresComputationChecks) {
    if (preMeasuresComputationChecks.length == 0) {
      return new PreMeasuresComputationChecksStep(analysisMetadataHolder, ceTaskMessages, configurationRepository);
    }
    return new PreMeasuresComputationChecksStep(analysisMetadataHolder, ceTaskMessages, configurationRepository, preMeasuresComputationChecks);
  }

  private void mockBranch(String branchName) {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn(branchName);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
  }

  private void mockPr(String pullRequestKey) {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getPullRequestKey()).thenReturn(pullRequestKey);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
  }

}

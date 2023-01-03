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
package org.sonar.scanner.repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonarqube.ws.NewCodePeriods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ForkDateSupplierTest {
  private final static String PROJECT_KEY = "project";
  private final static String BRANCH_KEY = "branch";
  private final static Path BASE_DIR = Paths.get("root");

  private NewCodePeriodLoader newCodePeriodLoader = mock(NewCodePeriodLoader.class);
  private BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private DefaultInputProject project = mock(DefaultInputProject.class);
  private ScmConfiguration scmConfiguration = mock(ScmConfiguration.class);
  private ScmProvider scmProvider = mock(ScmProvider.class);
  private ProjectBranches projectBranches = mock(ProjectBranches.class);
  private AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private ForkDateSupplier forkDateSupplier = new ForkDateSupplier(newCodePeriodLoader, branchConfiguration, project, scmConfiguration, projectBranches, analysisWarnings);

  @Before
  public void setUp() {
    when(projectBranches.isEmpty()).thenReturn(false);
    when(project.key()).thenReturn(PROJECT_KEY);
    when(project.getBaseDir()).thenReturn(BASE_DIR);
    when(scmConfiguration.isDisabled()).thenReturn(false);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
  }

  @Test
  public void returns_forkDate_for_branches_with_ref() {
    Instant date = Instant.now();
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(scmProvider.forkDate("master", BASE_DIR)).thenReturn(date);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "master"));

    assertThat(forkDateSupplier.get()).isEqualTo(date);
  }

  @Test
  public void uses_default_branch_if_no_branch_specified() {
    Instant date = Instant.now();
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(null);
    when(projectBranches.defaultBranchName()).thenReturn("default");
    when(newCodePeriodLoader.load(PROJECT_KEY, "default")).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "master"));
    when(scmProvider.forkDate("master", BASE_DIR)).thenReturn(date);

    assertThat(forkDateSupplier.get()).isEqualTo(date);

    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void returns_null_if_no_branches() {
    when(projectBranches.isEmpty()).thenReturn(true);

    assertThat(forkDateSupplier.get()).isNull();

    verify(branchConfiguration).isPullRequest();
    verify(projectBranches).isEmpty();
    verifyNoMoreInteractions(branchConfiguration);
    verifyNoInteractions(scmConfiguration, scmProvider, analysisWarnings, newCodePeriodLoader);
  }

  @Test
  public void returns_null_if_scm_disabled() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(scmConfiguration.isDisabled()).thenReturn(true);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "master"));

    assertThat(forkDateSupplier.get()).isNull();

    verify(scmConfiguration).isDisabled();
    verify(branchConfiguration, times(2)).branchName();
    verify(branchConfiguration).isPullRequest();
    verify(analysisWarnings).addUnique(anyString());

    verifyNoInteractions(scmProvider);
    verifyNoMoreInteractions(branchConfiguration);
  }

  @Test
  public void returns_null_if_reference_branch_is_the_branch_being_analyzed() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, BRANCH_KEY));

    assertThat(forkDateSupplier.get()).isNull();

    verify(branchConfiguration, times(2)).branchName();
    verify(branchConfiguration).isPullRequest();
    verify(newCodePeriodLoader).load(PROJECT_KEY, BRANCH_KEY);

    verifyNoInteractions(scmProvider, analysisWarnings, scmConfiguration);
    verifyNoMoreInteractions(branchConfiguration);
  }

  @Test
  public void returns_null_if_pull_request() {
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    assertThat(forkDateSupplier.get()).isNull();

    verify(branchConfiguration).isPullRequest();

    verifyNoInteractions(newCodePeriodLoader, analysisWarnings, scmProvider, scmConfiguration);
    verifyNoMoreInteractions(branchConfiguration);
  }

  @Test
  public void returns_null_if_new_code_period_is_not_ref() {
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "2"));

    assertThat(forkDateSupplier.get()).isNull();

    verifyNoInteractions(scmProvider, analysisWarnings, scmConfiguration);
  }

  private NewCodePeriods.ShowWSResponse createResponse(NewCodePeriods.NewCodePeriodType type, String value) {
    return NewCodePeriods.ShowWSResponse.newBuilder()
      .setType(type)
      .setValue(value)
      .build();
  }
}

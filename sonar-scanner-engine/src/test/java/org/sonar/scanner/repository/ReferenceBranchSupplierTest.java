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
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonarqube.ws.NewCodePeriods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReferenceBranchSupplierTest {
  private final static String PROJECT_KEY = "project";
  private final static String BRANCH_KEY = "branch";
  private final static Path BASE_DIR = Paths.get("root");

  private final NewCodePeriodLoader newCodePeriodLoader = mock(NewCodePeriodLoader.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final Configuration configuration = mock(Configuration.class);
  private final DefaultInputProject project = mock(DefaultInputProject.class);
  private final ProjectBranches projectBranches = mock(ProjectBranches.class);
  private final ReferenceBranchSupplier referenceBranchSupplier = new ReferenceBranchSupplier(configuration, newCodePeriodLoader, branchConfiguration, project, projectBranches);

  @Before
  public void setUp() {
    when(projectBranches.isEmpty()).thenReturn(false);
    when(project.key()).thenReturn(PROJECT_KEY);
    when(project.getBaseDir()).thenReturn(BASE_DIR);
    when(configuration.get("sonar.newCode.referenceBranch")).thenReturn(Optional.empty());
  }

  @Test
  public void get_returns_reference_branch_when_set() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "main"));

    assertThat(referenceBranchSupplier.get()).isEqualTo("main");
  }

  @Test
  public void get_uses_scanner_property_with_higher_priority() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "main"));

    when(configuration.get("sonar.newCode.referenceBranch")).thenReturn(Optional.of("master2"));

    assertThat(referenceBranchSupplier.get()).isEqualTo("master2");
  }

  @Test
  public void getFromProperties_uses_scanner_property() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(configuration.get("sonar.newCode.referenceBranch")).thenReturn(Optional.of("master2"));
    assertThat(referenceBranchSupplier.getFromProperties()).isEqualTo("master2");
  }

  @Test
  public void getFromProperties_returns_null_if_no_property() {
    assertThat(referenceBranchSupplier.getFromProperties()).isNull();
  }

  @Test
  public void getFromProperties_throws_ISE_if_reference_is_the_same_as_branch() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);

    when(configuration.get("sonar.newCode.referenceBranch")).thenReturn(Optional.of(BRANCH_KEY));
    assertThatThrownBy(referenceBranchSupplier::getFromProperties).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void get_uses_default_branch_if_no_branch_specified() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(null);
    when(projectBranches.defaultBranchName()).thenReturn("default");
    when(newCodePeriodLoader.load(PROJECT_KEY, "default")).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, "main"));

    assertThat(referenceBranchSupplier.get()).isEqualTo("main");
  }

  @Test
  public void get_returns_null_if_no_branches() {
    when(projectBranches.isEmpty()).thenReturn(true);

    assertThat(referenceBranchSupplier.get()).isNull();

    verify(branchConfiguration).isPullRequest();
    verify(projectBranches).isEmpty();
    verifyNoMoreInteractions(branchConfiguration);
    verifyNoInteractions(newCodePeriodLoader);
  }

  @Test
  public void get_returns_null_if_reference_branch_is_the_branch_being_analyzed() {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH, BRANCH_KEY));

    assertThat(referenceBranchSupplier.get()).isNull();

    verify(branchConfiguration, times(2)).branchName();
    verify(branchConfiguration, times(2)).isPullRequest();
    verify(newCodePeriodLoader).load(PROJECT_KEY, BRANCH_KEY);

    verifyNoMoreInteractions(branchConfiguration);
  }

  @Test
  public void get_returns_null_if_pull_request() {
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    assertThat(referenceBranchSupplier.get()).isNull();

    verify(branchConfiguration).isPullRequest();

    verifyNoInteractions(newCodePeriodLoader);
    verifyNoMoreInteractions(branchConfiguration);
  }

  @Test
  public void get_returns_null_if_new_code_period_is_not_ref() {
    when(branchConfiguration.isPullRequest()).thenReturn(true);
    when(branchConfiguration.branchName()).thenReturn(BRANCH_KEY);
    when(newCodePeriodLoader.load(PROJECT_KEY, BRANCH_KEY)).thenReturn(createResponse(NewCodePeriods.NewCodePeriodType.NUMBER_OF_DAYS, "2"));

    assertThat(referenceBranchSupplier.get()).isNull();
  }

  private NewCodePeriods.ShowWSResponse createResponse(NewCodePeriods.NewCodePeriodType type, String value) {
    return NewCodePeriods.ShowWSResponse.newBuilder()
      .setType(type)
      .setValue(value)
      .build();
  }
}

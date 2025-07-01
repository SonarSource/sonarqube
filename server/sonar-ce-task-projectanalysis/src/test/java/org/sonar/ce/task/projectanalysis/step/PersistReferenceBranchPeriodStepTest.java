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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.TestBranch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.period.PeriodOrigin;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;

class PersistReferenceBranchPeriodStepTest {

  private static final String MAIN_BRANCH = "main";
  private static final String FEATURE_BRANCH = "feature";
  private static final String BRANCH_UUID = "branch-uuid";
  private static final String PROJECT_NAME = "project-name";
  private static final String PROJECT_UUID = "project-uuid";

  private final PeriodHolder periodHolder = mock(PeriodHolder.class);

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  private final DbClient dbClient = mock(DbClient.class);

  private final TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private final PersistReferenceBranchPeriodStep persistReferenceBranchPeriodStep = new PersistReferenceBranchPeriodStep(
    periodHolder, analysisMetadataHolder, dbClient, treeRootHolder);

  private final DbSession dbSession = mock(DbSession.class);
  private final NewCodePeriodDao newCodePeriodeDao = mock(NewCodePeriodDao.class);

  private final ComputationStep.Context context = mock(ComputationStep.Context.class);

  @BeforeEach
  void setUp() {
    Project project = new Project(PROJECT_UUID, "project-key", PROJECT_NAME, "project-description", emptyList());
    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    when(analysisMetadataHolder.getProject()).thenReturn(project);
    when(analysisMetadataHolder.getBranch()).thenReturn(new TestBranch(FEATURE_BRANCH));

    when(periodHolder.hasPeriod()).thenReturn(true);
    Period period = new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), MAIN_BRANCH, null);
    when(periodHolder.getPeriod()).thenReturn(period);
    when(periodHolder.getPeriodOrigin()).thenReturn(PeriodOrigin.SCANNER);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.newCodePeriodDao()).thenReturn(newCodePeriodeDao);

    Component root = mock(Component.class);
    when(treeRootHolder.getRoot()).thenReturn(root);
    when(root.getUuid()).thenReturn(BRANCH_UUID);

  }

  @Test
  void getDescription() {
    assertThat(persistReferenceBranchPeriodStep.getDescription()).isEqualTo("Persist or update reference branch new code period");
  }

  @Test
  void execute_shouldDoNothing_whenNotABranch() {
    when(analysisMetadataHolder.isBranch()).thenReturn(false);
    verifyExecuteNotCalled();
  }

  @Test
  void execute_shouldDoNothing_whenNoPeriods() {
    when(periodHolder.hasPeriod()).thenReturn(false);
    verifyExecuteNotCalled();
  }

  @Test
  void execute_shouldDoNothing_whenNotReferenceBranchPeriod() {
    Period period = new Period("not-ref-branch", MAIN_BRANCH, null);
    when(periodHolder.getPeriod()).thenReturn(period);
    when(periodHolder.getPeriodOrigin()).thenReturn(PeriodOrigin.SETTINGS);
    verifyExecuteNotCalled();
  }

  private void verifyExecuteNotCalled() {
    PersistReferenceBranchPeriodStep spyStep = spy(persistReferenceBranchPeriodStep);

    spyStep.execute(context);

    verify(spyStep, never()).executePersistPeriodStep();
  }

  @Test
  void execute_shouldCreateNewCodePeriod_whenItDoesNotExists() {
    NewCodePeriodDto expectedNewCodePeriod = new NewCodePeriodDto()
      .setBranchUuid(BRANCH_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue(MAIN_BRANCH);
    when(newCodePeriodeDao.selectByBranch(dbSession, PROJECT_UUID, BRANCH_UUID)).thenReturn(Optional.empty());

    persistReferenceBranchPeriodStep.execute(context);

    assertThat(logs.logs(DEBUG)).contains(
      String.format("Persisting reference branch new code period '%s' for project '%s' and branch '%s'",MAIN_BRANCH, PROJECT_NAME, FEATURE_BRANCH));
    ArgumentCaptor<NewCodePeriodDto> newCodePeriodCaptor = ArgumentCaptor.forClass(NewCodePeriodDto.class);
    verify(newCodePeriodeDao).insert(eq(dbSession), newCodePeriodCaptor.capture());
    assertThat(newCodePeriodCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedNewCodePeriod);
  }

  @Test
  void execute_shouldUpdateNewCodePeriod_whenItExistsAndItChanged() {
    NewCodePeriodDto expectedNewCodePeriod = new NewCodePeriodDto()
      .setBranchUuid(BRANCH_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue(MAIN_BRANCH);
    var newCodePeriodInBase = new NewCodePeriodDto()
      .setBranchUuid(BRANCH_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue("old-value");

    when(newCodePeriodeDao.selectByBranch(dbSession, PROJECT_UUID, BRANCH_UUID)).thenReturn(Optional.of(newCodePeriodInBase));

    persistReferenceBranchPeriodStep.execute(context);

    assertThat(logs.logs(DEBUG)).contains(
      String.format("Updating reference branch new code period '%s' for project '%s' and branch '%s'", MAIN_BRANCH ,PROJECT_NAME, FEATURE_BRANCH));
    ArgumentCaptor<NewCodePeriodDto> newCodePeriodCaptor = ArgumentCaptor.forClass(NewCodePeriodDto.class);
    verify(newCodePeriodeDao).update(eq(dbSession), newCodePeriodCaptor.capture());
    assertThat(newCodePeriodCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedNewCodePeriod);
  }

  @Test
  void execute_shouldDoNothing_whenItExistsAndItDidNotChanged() {
    NewCodePeriodDto expectedNewCodePeriod = new NewCodePeriodDto()
      .setBranchUuid(BRANCH_UUID)
      .setProjectUuid(PROJECT_UUID)
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue(MAIN_BRANCH);

    when(newCodePeriodeDao.selectByBranch(dbSession, PROJECT_UUID, BRANCH_UUID)).thenReturn(Optional.of(expectedNewCodePeriod));

    persistReferenceBranchPeriodStep.execute(context);

    verify(newCodePeriodeDao, never()).update(any(), any());
    verify(newCodePeriodeDao, never()).insert(any(), any());
  }

}

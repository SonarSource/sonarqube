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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.period.NewCodeReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BranchComponentUuidsDelegateTest {

  @RegisterExtension
  private final PeriodHolderRule periodHolder = new PeriodHolderRule();

  private AnalysisMetadataHolder analysisMetadataHolder;
  private ReferenceBranchComponentUuids referenceBranchComponentUuids;
  private NewCodeReferenceBranchComponentUuids newCodeReferenceBranchComponentUuids;
  private BranchComponentUuidsDelegate underTest;
  private Branch branch;

  @BeforeEach
  void setUp() {
    periodHolder.setPeriod(null);
    analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
    referenceBranchComponentUuids = mock(ReferenceBranchComponentUuids.class);
    newCodeReferenceBranchComponentUuids = mock(NewCodeReferenceBranchComponentUuids.class);
    branch = mock(Branch.class);
    underTest = new BranchComponentUuidsDelegate(analysisMetadataHolder, periodHolder, referenceBranchComponentUuids, newCodeReferenceBranchComponentUuids);
  }

  @Test
  void getComponentUuid_returns_referenceBranchComponentUuid_for_pull_request() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(referenceBranchComponentUuids.getComponentUuid("key")).thenReturn("referenceUuid");

    String result = underTest.getComponentUuid("key");

    assertEquals("referenceUuid", result);
  }

  @Test
  void getComponentUuid_returns_newCodeReferenceBranchComponentUuid_for_new_code_period_reference_branch() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "newCodeReferenceBranchName", null));
    when(newCodeReferenceBranchComponentUuids.getComponentUuid("key")).thenReturn("newCodeReferenceUuid");

    String result = underTest.getComponentUuid("key");

    assertEquals("newCodeReferenceUuid", result);
  }

  @Test
  void getComponentUuid_returns_referenceBranchComponentUuid_for_non_main_branch() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(branch.isMain()).thenReturn(false);
    when(referenceBranchComponentUuids.getComponentUuid("key")).thenReturn("referenceUuid");

    String result = underTest.getComponentUuid("key");

    assertEquals("referenceUuid", result);
  }

  @Test
  void getComponentUuid_returns_null_for_main_branch() {
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(branch.isMain()).thenReturn(true);

    String result = underTest.getComponentUuid("key");

    assertNull(result);
  }

  @Test
  void getReferenceBranchName_returns_newCodePeriodReferenceBranchName() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "newCodeReferenceBranchName", null));

    String result = underTest.getReferenceBranchName();

    assertEquals("newCodeReferenceBranchName", result);
  }

  @Test
  void getReferenceBranchName_returns_referenceBranchName_for_non_main_branch() {
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(branch.isMain()).thenReturn(false);
    when(referenceBranchComponentUuids.getReferenceBranchName()).thenReturn("referenceBranchName");

    String result = underTest.getReferenceBranchName();

    assertEquals("referenceBranchName", result);
  }

  @Test
  void getReferenceBranchName_returns_main_branch_name() {
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(branch.isMain()).thenReturn(true);
    when(analysisMetadataHolder.getBranch().getName()).thenReturn("mainBranchName");

    String result = underTest.getReferenceBranchName();

    assertEquals("mainBranchName", result);
  }
}

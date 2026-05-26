/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchComponentUuidsDelegate;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.ce.task.projectanalysis.filemove.MutableMovedFilesRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

class OriginalFileResolverTest {

  private static final String FILE_KEY = "file:key";
  private static final String FILE_UUID = "file-uuid";
  private static final String DELEGATE_UUID = "delegate-uuid";

  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private final BranchComponentUuidsDelegate branchComponentUuidsDelegate = mock(BranchComponentUuidsDelegate.class);

  @RegisterExtension
  private final PeriodHolderRule periodHolder = new PeriodHolderRule();

  @RegisterExtension
  private final MutableMovedFilesRepositoryRule movedFiles = new MutableMovedFilesRepositoryRule();

  private final OriginalFileResolver underTest = new OriginalFileResolver(analysisMetadataHolder, movedFiles, branchComponentUuidsDelegate);

  private final Component file = builder(FILE, 1).setKey(FILE_KEY).setUuid(FILE_UUID).build();

  @Test
  void pr_mode_returns_delegate_uuid() {
    periodHolder.setPeriod(null);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(DELEGATE_UUID);
  }

  @Test
  void pr_mode_returns_empty_when_delegate_returns_null() {
    periodHolder.setPeriod(null);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(null);

    assertThat(underTest.getFileUuid(file)).isEmpty();
  }

  @Test
  void reference_branch_ncd_first_analysis_returns_delegate_uuid() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "main", null));
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(DELEGATE_UUID);
  }

  @Test
  void reference_branch_ncd_subsequent_analysis_returns_own_uuid() {
    // OriginalFileResolver does not special-case REFERENCE_BRANCH NCD; that logic lives in ScmInfoRepositoryImpl
    // and is plumbed via a flag into ScmInfoDbLoader / SourceLinesDiff. On subsequent analyses the resolver
    // falls through to the file's own UUID.
    periodHolder.setPeriod(new Period(NewCodePeriodType.REFERENCE_BRANCH.name(), "main", null));
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(FILE_UUID);
  }

  @Test
  void number_of_days_ncd_first_analysis_returns_delegate_uuid() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "30", null));
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(DELEGATE_UUID);
  }

  @Test
  void number_of_days_ncd_subsequent_analysis_falls_back_to_own_uuid() {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "30", null));
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(FILE_UUID);
  }

  @Test
  void no_period_falls_back_to_own_uuid() {
    periodHolder.setPeriod(null);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains(FILE_UUID);
  }

  @Test
  void returns_moved_file_uuid_when_file_was_moved() {
    periodHolder.setPeriod(null);
    movedFiles.setOriginalFile(file, new MovedFilesRepository.OriginalFile("original-uuid", "original:key"));

    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file)).contains("original-uuid");
  }

  @Test
  void useReferenceBranchForNcd_flag_returns_delegate_uuid_regardless_of_analysis_state() {
    // SONAR-27766: when the caller passes useReferenceBranchForNcd=true, the resolver returns the
    // reference-branch counterpart unconditionally — bypassing isFirstAnalysis / move-detection logic.
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(DELEGATE_UUID);

    assertThat(underTest.getFileUuid(file, true)).contains(DELEGATE_UUID);
  }

  @Test
  void useReferenceBranchForNcd_flag_returns_empty_when_reference_branch_does_not_have_the_file() {
    when(branchComponentUuidsDelegate.getComponentUuid(FILE_KEY)).thenReturn(null);

    assertThat(underTest.getFileUuid(file, true)).isEmpty();
  }
}

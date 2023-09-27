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
package org.sonar.scanner.externalissue.sarif;

import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Sarif210;
import org.sonar.scanner.externalissue.sarif.RunMapper.RunMapperResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSarif210ImporterTest extends TestCase {

  @Mock
  private RunMapper runMapper;

  @Rule
  public LogTester logTester = new LogTester();

  @InjectMocks
  DefaultSarif210Importer sarif210Importer;

  @Test
  public void importSarif_shouldDelegateRunMapping_toRunMapper() {
    Sarif210 sarif210 = mock(Sarif210.class);

    Run run1 = mock(Run.class);
    Run run2 = mock(Run.class);
    when(sarif210.getRuns()).thenReturn(Set.of(run1, run2));

    NewExternalIssue issue1run1 = mock(NewExternalIssue.class);
    NewExternalIssue issue2run1 = mock(NewExternalIssue.class);
    NewExternalIssue issue1run2 = mock(NewExternalIssue.class);
    when(runMapper.mapRun(run1)).thenReturn(new RunMapperResult().newExternalIssues(List.of(issue1run1, issue2run1)));
    when(runMapper.mapRun(run2)).thenReturn(new RunMapperResult().newExternalIssues(List.of(issue1run2)));

    SarifImportResults sarifImportResults = sarif210Importer.importSarif(sarif210);

    assertThat(sarifImportResults.getSuccessFullyImportedIssues()).isEqualTo(3);
    assertThat(sarifImportResults.getSuccessFullyImportedRuns()).isEqualTo(2);
    assertThat(sarifImportResults.getFailedRuns()).isZero();
    verify(issue1run1).save();
    verify(issue2run1).save();
    verify(issue1run2).save();
  }

  @Test
  public void importSarif_whenExceptionThrownByRunMapper_shouldLogAndContinueProcessing() {
    Sarif210 sarif210 = mock(Sarif210.class);

    Run run1 = mock(Run.class);
    Run run2 = mock(Run.class);
    when(sarif210.getRuns()).thenReturn(Set.of(run1, run2));

    Exception testException = new RuntimeException("test");
    when(runMapper.mapRun(run1)).thenThrow(testException);
    NewExternalIssue issue1run2 = mock(NewExternalIssue.class);
    when(runMapper.mapRun(run2)).thenReturn(new RunMapperResult().newExternalIssues(List.of(issue1run2)));

    SarifImportResults sarifImportResults = sarif210Importer.importSarif(sarif210);

    assertThat(sarifImportResults.getSuccessFullyImportedIssues()).isOne();
    assertThat(sarifImportResults.getSuccessFullyImportedRuns()).isOne();
    assertThat(sarifImportResults.getFailedRuns()).isOne();
    assertThat(logTester.logs(Level.WARN)).containsOnly("Failed to import a sarif run, error: " + testException.getMessage());
    verify(issue1run2).save();
  }

  @Test
  public void importSarif_whenGetRunsReturnNull_shouldFailWithProperMessage() {
    Sarif210 sarif210 = mock(Sarif210.class);

    when(sarif210.getRuns()).thenReturn(null);

    assertThatNullPointerException()
      .isThrownBy(() -> sarif210Importer.importSarif(sarif210))
      .withMessage("The runs section of the Sarif report is null");

    verifyNoInteractions(runMapper);
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.log.CeTaskMessages.Message;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PerformNotAnalyzedFilesCheckStepTest {

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final CeTaskMessages ceTaskMessages = mock(CeTaskMessages.class);
  private final PerformNotAnalyzedFilesCheckStep underTest = new PerformNotAnalyzedFilesCheckStep(reportReader, ceTaskMessages, editionProvider, System2.INSTANCE);

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo(PerformNotAnalyzedFilesCheckStep.DESCRIPTION);
  }

  @Test
  public void execute_adds_warning_in_SQ_community_edition_if_there_are_c_or_cpp_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder()
      .putNotAnalyzedFilesByLanguage("C++", 20)
      .putNotAnalyzedFilesByLanguage("C", 10)
      .putNotAnalyzedFilesByLanguage("SomeLang", 1000)
      .build());
    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues())
      .extracting(Message::getText, Message::isDismissible)
      .containsExactly(tuple(
        "10 C, 20 C++ and 1000 SomeLang file(s) detected during the last analysis. C, C++ and SomeLang code cannot be analyzed with SonarQube community " +
          "edition. Please consider <a href=\"https://www.sonarqube.org/trial-request/developer-edition/?referrer=sonarqube-cpp\">upgrading to the Developer " +
          "Edition</a> to analyze this language.",
        true));
  }

  @Test
  public void execute_adds_warning_in_SQ_community_edition_if_there_are_c_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder()
      .putNotAnalyzedFilesByLanguage("C", 10)
      .build());
    ArgumentCaptor<CeTaskMessages.Message> argumentCaptor = ArgumentCaptor.forClass(CeTaskMessages.Message.class);

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    List<CeTaskMessages.Message> messages = argumentCaptor.getAllValues();
    assertThat(messages).extracting(CeTaskMessages.Message::getText).containsExactly(
      "10 C file(s) detected during the last analysis. C code cannot be analyzed with SonarQube community " +
        "edition. Please consider <a href=\"https://www.sonarqube.org/trial-request/developer-edition/?referrer=sonarqube-cpp\">upgrading to the Developer " +
        "Edition</a> to analyze this language.");
  }

  @Test
  public void execute_adds_warning_in_SQ_community_edition_if_there_are_cpp_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder()
      .putNotAnalyzedFilesByLanguage("C++", 9)
      .build());
    ArgumentCaptor<CeTaskMessages.Message> argumentCaptor = ArgumentCaptor.forClass(CeTaskMessages.Message.class);

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    List<CeTaskMessages.Message> messages = argumentCaptor.getAllValues();
    assertThat(messages).extracting(CeTaskMessages.Message::getText).containsExactly(
      "9 C++ file(s) detected during the last analysis. C++ code cannot be analyzed with SonarQube community " +
        "edition. Please consider <a href=\"https://www.sonarqube.org/trial-request/developer-edition/?referrer=sonarqube-cpp\">upgrading to the Developer " +
        "Edition</a> to analyze this language.");
  }

  @Test
  public void execute_does_not_add_a_warning_in_SQ_community_edition_if_cpp_files_in_report_is_zero() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().putNotAnalyzedFilesByLanguage("C++", 0).build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, never()).add(any());
  }

  @Test
  public void execute_does_not_add_a_warning_in_SQ_community_edition_if_no_c_or_cpp_files_2() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, never()).add(any());
  }

  @Test
  public void execute_does_not_add_a_warning_in_SQ_non_community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.ENTERPRISE));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().putNotAnalyzedFilesByLanguage("C++", 20).build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, never()).add(any());
  }
}

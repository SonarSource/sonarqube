/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.language;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.log.CeTaskMessages.Message;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.dismissmessage.MessageType;
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
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;

@RunWith(MockitoJUnitRunner.class)
public class HandleUnanalyzedLanguagesStepTest {

  private static final int PROJECT_REF = 1;
  private static final Component ROOT_PROJECT = ReportComponent.builder(PROJECT, PROJECT_REF).build();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(ROOT_PROJECT);
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(UNANALYZED_C)
    .add(UNANALYZED_CPP);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Captor
  private ArgumentCaptor<Message> argumentCaptor;

  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final CeTaskMessages ceTaskMessages = mock(CeTaskMessages.class);

  private final HandleUnanalyzedLanguagesStep underTest = new HandleUnanalyzedLanguagesStep(reportReader, ceTaskMessages, editionProvider, System2.INSTANCE, treeRootHolder,
    metricRepository, measureRepository);

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo(HandleUnanalyzedLanguagesStep.DESCRIPTION);
  }

  @Test
  public void add_warning_and_measures_in_SQ_community_edition_if_there_are_c_or_cpp_files() {
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

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues())
      .extracting(Message::getText, Message::getType)
      .containsExactly(tuple(
        "10 unanalyzed C, 20 unanalyzed C++ and 1000 unanalyzed SomeLang files were detected in this project during the last analysis. C," +
          " C++ and SomeLang cannot be analyzed with your current SonarQube edition. Please consider" +
          " <a target=\"_blank\" href=\"https://www.sonarsource.com/plans-and-pricing/developer/?referrer=sonarqube-cpp\">upgrading to Developer Edition</a> to find Bugs," +
          " Code Smells, Vulnerabilities and Security Hotspots in these files.",
        MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE));
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY).get().getIntValue()).isEqualTo(10);
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY).get().getIntValue()).isEqualTo(20);
  }

  @Test
  public void adds_warning_and_measures_in_SQ_community_edition_if_there_are_c_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder()
      .putNotAnalyzedFilesByLanguage("C", 10)
      .build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    List<CeTaskMessages.Message> messages = argumentCaptor.getAllValues();
    assertThat(messages).extracting(CeTaskMessages.Message::getText).containsExactly(
      "10 unanalyzed C files were detected in this project during the last analysis. C cannot be analyzed with your current SonarQube edition. Please" +
        " consider <a target=\"_blank\" href=\"https://www.sonarsource.com/plans-and-pricing/developer/?referrer=sonarqube-cpp\">upgrading to Developer" +
        " Edition</a> to find Bugs, Code Smells, Vulnerabilities and Security Hotspots in this file.");
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY).get().getIntValue()).isEqualTo(10);
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY)).isEmpty();
  }

  @Test
  public void adds_warning_in_SQ_community_edition_if_there_are_cpp_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder()
      .putNotAnalyzedFilesByLanguage("C++", 1)
      .build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, times(1)).add(argumentCaptor.capture());
    List<CeTaskMessages.Message> messages = argumentCaptor.getAllValues();
    assertThat(messages).extracting(CeTaskMessages.Message::getText).containsExactly(
      "1 unanalyzed C++ file was detected in this project during the last analysis. C++ cannot be analyzed with your current SonarQube edition. Please" +
        " consider <a target=\"_blank\" href=\"https://www.sonarsource.com/plans-and-pricing/developer/?referrer=sonarqube-cpp\">upgrading to Developer" +
        " Edition</a> to find Bugs, Code Smells, Vulnerabilities and Security Hotspots in this file.");
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY).get().getIntValue()).isOne();
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY)).isEmpty();
  }

  @Test
  public void do_nothing_SQ_community_edition_if_cpp_files_in_report_is_zero() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().putNotAnalyzedFilesByLanguage("C++", 0).build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, never()).add(any());

    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY)).isEmpty();
  }

  @Test
  public void execute_does_not_add_a_warning_in_SQ_community_edition_if_no_c_or_cpp_files() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    reportReader.setAnalysisWarnings(warnings);
    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());

    underTest.execute(new TestComputationStepContext());

    verify(ceTaskMessages, never()).add(any());
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY)).isEmpty();
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
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_C_KEY)).isEmpty();
    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, UNANALYZED_CPP_KEY)).isEmpty();
  }
}

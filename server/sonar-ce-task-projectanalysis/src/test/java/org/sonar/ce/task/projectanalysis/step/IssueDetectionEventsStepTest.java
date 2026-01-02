/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.language.LanguageRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IssueDetectionEventsStepTest {
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private static final String QP_NAME_1 = "qp_1";
  private static final String QP_NAME_2 = "qp_2";
  private static final String LANGUAGE_KEY_1 = "language_key1";
  private static final String LANGUAGE_KEY_2 = "language_key2";

  private final MetricRepository metricRepository = mock(MetricRepository.class);
  private final MeasureRepository measureRepository = mock(MeasureRepository.class);
  private final LanguageRepository languageRepository = mock(LanguageRepository.class);
  private final EventRepository eventRepository = mock(EventRepository.class);
  private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  private final Metric qualityProfileMetric = mock(Metric.class);

  private final IssueDetectionEventsStep underTest = new IssueDetectionEventsStep(treeRootHolder, metricRepository, measureRepository, languageRepository, eventRepository,
    analysisMetadataHolder);

  @Before
  public void setUp() {
    when(metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY)).thenReturn(qualityProfileMetric);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid").setKey("key").build());
  }

  @Test
  public void execute_whenNoBaseMeasure_shouldNotRaiseEvent() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void execute_whenNoRawMeasure_shouldNotRaiseEvent() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void execute_whenNoBaseMeasureAndQPMeasureIsEmpty_shouldNotRaiseEvent() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void execute_whenAnalyzerChangedAndLanguageNotSupported_shouldSkipRaisingEvent() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());

    mockLanguageInRepository(LANGUAGE_KEY_1);
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure(qp1)));

    when(analysisMetadataHolder.getScannerPluginsByKey()).thenReturn(Collections.emptyMap());
    when(analysisMetadataHolder.getBaseAnalysis()).thenReturn(new Analysis.Builder().setUuid("uuid").setCreatedAt(1L).build());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void execute_whenAnalyzerChanged_shouldRaiseEventForAllLanguages() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    QualityProfile qp2 = qp(QP_NAME_2, LANGUAGE_KEY_2, new Date());

    mockLanguageInRepository(LANGUAGE_KEY_1);
    mockLanguageInRepository(LANGUAGE_KEY_2);

    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure(qp1, qp2)));

    ScannerPlugin scannerPluginLanguage1 = mockScannerPlugin(LANGUAGE_KEY_1, 3L);
    ScannerPlugin scannerPluginLanguage2 = mockScannerPlugin(LANGUAGE_KEY_2, 2L);

    when(analysisMetadataHolder.getScannerPluginsByKey()).thenReturn(Map.of(LANGUAGE_KEY_1, scannerPluginLanguage1, LANGUAGE_KEY_2, scannerPluginLanguage2));
    when(analysisMetadataHolder.getBaseAnalysis()).thenReturn(new Analysis.Builder().setUuid("uuid").setCreatedAt(1L).build());

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository, times(2)).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);

    assertThat(eventArgumentCaptor.getAllValues())
      .extracting(Event::getCategory, Event::getName, Event::getDescription)
      .containsExactlyInAnyOrder(tuple(Event.Category.ISSUE_DETECTION, "Capabilities have been updated (language_key1_name)", null),
        tuple(Event.Category.ISSUE_DETECTION, "Capabilities have been updated (language_key2_name)", null));
  }

  @Test
  public void execute_whenAnalyzerChangedAndAnalyzerUpdateDateBeforeAnalysis_shouldNotRaiseEvent() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());

    mockLanguageInRepository(LANGUAGE_KEY_1);

    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure(qp1)));

    ScannerPlugin scannerPluginLanguage1 = mockScannerPlugin(LANGUAGE_KEY_1, 1L);

    when(analysisMetadataHolder.getScannerPluginsByKey()).thenReturn(Map.of(LANGUAGE_KEY_1, scannerPluginLanguage1));
    when(analysisMetadataHolder.getBaseAnalysis()).thenReturn(new Analysis.Builder().setUuid("uuid").setCreatedAt(3L).build());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void execute_whenAnalyzerDidNotChange_shouldNotRaiseEvent() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());

    mockLanguage1AsNotInRepository();

    when(analysisMetadataHolder.getBaseAnalysis()).thenReturn(new Analysis.Builder().setUuid("uuid").setCreatedAt(1L).build());

    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure(qp)));

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  private void mockLanguageInRepository(String languageKey) {
    Language language = new AbstractLanguage(languageKey, languageKey + "_name") {
      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    };
    when(languageRepository.find(languageKey)).thenReturn(Optional.of(language));
  }

  private void mockLanguage1AsNotInRepository() {
    when(languageRepository.find(LANGUAGE_KEY_1)).thenReturn(Optional.empty());
  }

  private static QualityProfile qp(String qpName, String languageKey, Date date) {
    return new QualityProfile(qpName + "-" + languageKey, qpName, languageKey, date);
  }

  private static Measure newMeasure(@Nullable QualityProfile... qps) {
    return Measure.newMeasureBuilder().create(toJson(qps));
  }

  private static String toJson(@Nullable QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = qps != null ? Arrays.asList(qps) : Collections.emptyList();
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }

  private static ScannerPlugin mockScannerPlugin(String repositoryKey, long updatedAt) {
    ScannerPlugin scannerPluginLanguage = mock(ScannerPlugin.class);
    when(scannerPluginLanguage.getUpdatedAt()).thenReturn(updatedAt);
    when(scannerPluginLanguage.getKey()).thenReturn(repositoryKey);
    return scannerPluginLanguage;
  }
}

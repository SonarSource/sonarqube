/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.MutableQProfileStatusRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.RegisterQualityProfileStatusStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.ADDED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.REMOVED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UNCHANGED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UPDATED;


public class RegisterQualityProfileStatusStepTest {

  private static final String QP_NAME_1 = "qp_1";
  private static final String QP_NAME_2 = "qp_2";
  private static final String LANGUAGE_KEY_1 = "language_key1";
  private static final String LANGUAGE_KEY_2 = "language_key_2";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private MetricRepository metricRepository = mock(MetricRepository.class);
  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private MutableQProfileStatusRepository qProfileStatusRepository = mock(MutableQProfileStatusRepository.class);
  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private Metric qualityProfileMetric = mock(Metric.class);

  private RegisterQualityProfileStatusStep underTest = new RegisterQualityProfileStatusStep(treeRootHolder, measureRepository, metricRepository, qProfileStatusRepository, analysisMetadataHolder);

  @Before
  public void setUp() {
    when(metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY)).thenReturn(qualityProfileMetric);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid").setKey("key").build());
  }

  @Test
  public void register_nothing_if_no_base_measure() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  @Test
  public void register_nothing_if_no_base_and_quality_profile_measure_is_empty() {
    mockBaseQPMeasures(treeRootHolder.getRoot(), null);

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  @Test
  public void register_removed_profile() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());

    mockBaseQPMeasures(treeRootHolder.getRoot(), new QualityProfile[] {qp});

    underTest.execute(new TestComputationStepContext());

    verify(qProfileStatusRepository).register(eq(qp.getQpKey()), eq(REMOVED));
    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  @Test
  public void register_added_profile() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date(1000L));
    QualityProfile qp2 = qp(QP_NAME_2, LANGUAGE_KEY_2, new Date(1000L));

    mockBaseQPMeasures(treeRootHolder.getRoot(), arrayOf(qp1));
    mockRawQProfiles(ImmutableList.of(qp1, qp2));
    underTest.execute(new TestComputationStepContext());

    verify(qProfileStatusRepository).register(eq(qp1.getQpKey()), eq(UNCHANGED));
    verify(qProfileStatusRepository).register(eq(qp2.getQpKey()), eq(ADDED));
    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  @Test
  public void register_updated_profile() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date(1000L));
    QualityProfile qp2 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date(1200L));

    mockBaseQPMeasures(treeRootHolder.getRoot(), arrayOf(qp1));
    mockRawQProfiles(ImmutableList.of(qp2));
    underTest.execute(new TestComputationStepContext());

    verify(qProfileStatusRepository).register(eq(qp2.getQpKey()), eq(UPDATED));
    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  @Test
  public void register_unchanged_profile() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date(1000L));

    mockBaseQPMeasures(treeRootHolder.getRoot(), arrayOf(qp1));
    mockRawQProfiles(ImmutableList.of(qp1));
    underTest.execute(new TestComputationStepContext());

    verify(qProfileStatusRepository).register(eq(qp1.getQpKey()), eq(UNCHANGED));
    verifyNoMoreInteractions(qProfileStatusRepository);
  }

  private void mockBaseQPMeasures(Component component, @Nullable QualityProfile[] previous) {
    when(measureRepository.getBaseMeasure(component, qualityProfileMetric)).thenReturn(Optional.of(newMeasure(previous)));
  }

  private void mockRawQProfiles(@Nullable List<QualityProfile> previous) {
    Map<String, QualityProfile> qpByLanguages = previous.stream().collect(Collectors.toMap(QualityProfile::getLanguageKey, q -> q));
    when(analysisMetadataHolder.getQProfilesByLanguage()).thenReturn(qpByLanguages);
  }

  private static QualityProfile qp(String qpName, String languageKey, Date date) {
    return new QualityProfile(qpName + "-" + languageKey, qpName, languageKey, date);
  }

  private static QualityProfile[] arrayOf(QualityProfile... qps) {
    return qps;
  }

  private static Measure newMeasure(@Nullable QualityProfile... qps) {
    return Measure.newMeasureBuilder().create(toJson(qps));
  }

  private static String toJson(@Nullable QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = qps == null ? Collections.emptyList() : Arrays.asList(qps);
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }

}

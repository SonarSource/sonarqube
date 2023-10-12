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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.testfixtures.log.LogTester;
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
import org.sonar.ce.task.projectanalysis.qualityprofile.MutableQProfileStatusRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepositoryImpl;
import org.sonar.ce.task.projectanalysis.qualityprofile.QualityProfileRuleChangeResolver;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.ADDED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.REMOVED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UNCHANGED;
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UPDATED;

public class QualityProfileEventsStepTest {
  private static final Date BEFORE_DATE = parseDateTime("2011-04-25T01:05:13+0100");
  private static final Date AFTER_DATE = parseDateTime("2011-04-25T01:05:17+0100");
  private static final Date BEFORE_DATE_PLUS_1_SEC = parseDateTime("2011-04-25T01:05:14+0100");
  private static final Date AFTER_DATE_PLUS_1_SEC = parseDateTime("2011-04-25T01:05:18+0100");
  private static final String RULE_CHANGE_TEXT = "1 new rule, 2 deactivated rules and 3 modified rules";
  private static final Map<ActiveRuleChange.Type, Long> CHANGE_TO_NUMBER_OF_RULES_MAP = Map.ofEntries(
    Map.entry(ActiveRuleChange.Type.ACTIVATED, 1L),
    Map.entry(ActiveRuleChange.Type.DEACTIVATED, 2L),
    Map.entry(ActiveRuleChange.Type.UPDATED, 3L)
  );
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public LogTester logTester = new LogTester();

  private static final String QP_NAME_1 = "qp_1";
  private static final String QP_NAME_2 = "qp_2";
  private static final String LANGUAGE_KEY_1 = "language_key1";
  private static final String LANGUAGE_KEY_2 = "language_key_2";
  private static final String LANGUAGE_KEY_3 = "languageKey3";

  private final MetricRepository metricRepository = mock(MetricRepository.class);
  private final MeasureRepository measureRepository = mock(MeasureRepository.class);
  private final LanguageRepository languageRepository = mock(LanguageRepository.class);
  private final EventRepository eventRepository = mock(EventRepository.class);
  private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  private final MutableQProfileStatusRepository qProfileStatusRepository = new QProfileStatusRepositoryImpl();

  private final Metric qualityProfileMetric = mock(Metric.class);
  private final QualityProfileRuleChangeResolver qualityProfileRuleChangeTextResolver = mock(QualityProfileRuleChangeResolver.class);
  private final QualityProfileEventsStep underTest = new QualityProfileEventsStep(treeRootHolder, metricRepository, measureRepository, languageRepository, eventRepository, qProfileStatusRepository, qualityProfileRuleChangeTextResolver);

  @Before
  public void setUp() {
    when(metricRepository.getByKey(CoreMetrics.QUALITY_PROFILES_KEY)).thenReturn(qualityProfileMetric);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid").setKey("key").build());
  }

  @Test
  public void no_event_if_no_base_measure() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void no_event_if_no_raw_measure() {
    when(measureRepository.getBaseMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.of(newMeasure()));
    when(measureRepository.getRawMeasure(treeRootHolder.getRoot(), qualityProfileMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void no_event_if_no_base_and_quality_profile_measure_is_empty() {
    mockQualityProfileMeasures(treeRootHolder.getRoot(), null, null);

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void added_event_if_qp_is_added() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    qProfileStatusRepository.register(qp.getQpKey(), ADDED);

    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), null, arrayOf(qp));

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Use \"" + qp.getQpName() + "\" (" + language.getName() + ")", null, null);
  }

  @Test
  public void added_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    qProfileStatusRepository.register(qp.getQpKey(), ADDED);

    mockLanguageNotInRepository(LANGUAGE_KEY_1);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), null, arrayOf(qp));

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Use \"" + qp.getQpName() + "\" (" + qp.getLanguageKey() + ")", null, null);
  }

  @Test
  public void no_more_used_event_if_qp_is_removed() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    qProfileStatusRepository.register(qp.getQpKey(), REMOVED);

    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(qp), null);
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Stop using \"" + qp.getQpName() + "\" (" + language.getName() + ")", null, null);
  }

  @Test
  public void no_more_used_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    qProfileStatusRepository.register(qp.getQpKey(), REMOVED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(qp), null);
    mockLanguageNotInRepository(LANGUAGE_KEY_1);

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Stop using \"" + qp.getQpName() + "\" (" + qp.getLanguageKey() + ")", null, null);
  }

  @Test
  public void no_event_if_qp_is_unchanged() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1, new Date());
    qProfileStatusRepository.register(qp.getQpKey(), UNCHANGED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(qp), arrayOf(qp));

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository, never()).add(any(Event.class));
  }

  @Test
  public void changed_event_if_qp_has_been_updated() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, BEFORE_DATE);
    QualityProfile qp2 = qp(QP_NAME_1, LANGUAGE_KEY_1, AFTER_DATE);
    qProfileStatusRepository.register(qp2.getQpKey(), UPDATED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(qp1), arrayOf(qp2));
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    when(qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(qp2, treeRootHolder.getRoot().getUuid())).thenReturn(CHANGE_TO_NUMBER_OF_RULES_MAP);

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(),
      "\"" + qp2.getQpName() + "\" (" + language.getName() + ") updated with " + RULE_CHANGE_TEXT,
      "from=" + UtcDateUtils.formatDateTime(BEFORE_DATE_PLUS_1_SEC) +
        ";key=" + qp1.getQpKey() +
        ";languageKey=" + qp2.getLanguageKey()+
        ";name=" + qp2.getQpName() +
        ";to=" + UtcDateUtils.formatDateTime(AFTER_DATE_PLUS_1_SEC),
      RULE_CHANGE_TEXT);
  }

  @Test
  public void givenRulesWhereAddedModifiedOrRemoved_whenEventStep_thenQPChangeEventIsAddedWithDetails() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, BEFORE_DATE);
    QualityProfile qp2 = qp(QP_NAME_1, LANGUAGE_KEY_1, AFTER_DATE);

    // mock updated profile
    qProfileStatusRepository.register(qp2.getQpKey(), UPDATED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(qp1), arrayOf(qp2));
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    // mock rule changes
    when(qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(qp2, treeRootHolder.getRoot().getUuid())).thenReturn(CHANGE_TO_NUMBER_OF_RULES_MAP);

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(),
      "\"" + qp2.getQpName() + "\" (" + language.getName() + ") updated with " + RULE_CHANGE_TEXT,
      "from=" + UtcDateUtils.formatDateTime(BEFORE_DATE_PLUS_1_SEC) +
        ";key=" + qp1.getQpKey() +
        ";languageKey=" + qp2.getLanguageKey()+
        ";name=" + qp2.getQpName() +
        ";to=" + UtcDateUtils.formatDateTime(AFTER_DATE_PLUS_1_SEC),
      RULE_CHANGE_TEXT);
  }

  @Test
  public void givenRuleTextResolverException_whenEventStep_thenLogAndContinue() {
    // given
    logTester.setLevel(Level.ERROR);
    QualityProfile existingQP = qp(QP_NAME_1, LANGUAGE_KEY_1, BEFORE_DATE);
    QualityProfile newQP = qp(QP_NAME_1, LANGUAGE_KEY_1, AFTER_DATE);

    // mock updated profile
    qProfileStatusRepository.register(newQP.getQpKey(), UPDATED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(existingQP), arrayOf(newQP));

    when(qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(newQP, treeRootHolder.getRoot().getUuid())).thenThrow(new RuntimeException("error"));
    var context = new TestComputationStepContext();

    // when
    underTest.execute(context);

    // then
    assertThat(logTester.logs(Level.ERROR)).containsExactly("Failed to generate 'change' event for Quality Profile " + newQP.getQpKey());
    verify(eventRepository, never()).add(any(Event.class));
  }

  @Test
  public void givenNoChangesFound_whenEventStep_thenDebugLogAndSkipEvent() {
    // given
    logTester.setLevel(Level.DEBUG);
    QualityProfile existingQP = qp(QP_NAME_1, LANGUAGE_KEY_1, BEFORE_DATE);
    QualityProfile newQP = qp(QP_NAME_1, LANGUAGE_KEY_1, AFTER_DATE);

    // mock updated profile
    qProfileStatusRepository.register(newQP.getQpKey(), UPDATED);
    mockQualityProfileMeasures(treeRootHolder.getRoot(), arrayOf(existingQP), arrayOf(newQP));

    when(qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(newQP, treeRootHolder.getRoot().getUuid())).thenReturn(Collections.emptyMap());
    var context = new TestComputationStepContext();

    // when
    underTest.execute(context);

    // then
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("No changes found for Quality Profile " + newQP.getQpKey() + ". Quality Profile event skipped.");
    verify(eventRepository, never()).add(any(Event.class));
  }

  @Test
  public void verify_detection_with_complex_mix_of_qps() {
    final Set<Event> events = new HashSet<>();
    doAnswer(invocationOnMock -> {
      events.add((Event) invocationOnMock.getArguments()[0]);
      return null;
    }).when(eventRepository).add(any(Event.class));

    Date date = new Date();
    QualityProfile qp1 = qp(QP_NAME_2, LANGUAGE_KEY_1, date);
    QualityProfile qp2 = qp(QP_NAME_2, LANGUAGE_KEY_2, date);
    QualityProfile qp3 = qp(QP_NAME_1, LANGUAGE_KEY_1, BEFORE_DATE);
    QualityProfile qp3_updated = qp(QP_NAME_1, LANGUAGE_KEY_1, AFTER_DATE);
    QualityProfile qp4 = qp(QP_NAME_2, LANGUAGE_KEY_3, date);

    mockQualityProfileMeasures(
      treeRootHolder.getRoot(),
      arrayOf(qp1, qp2, qp3),
      arrayOf(qp3_updated, qp2, qp4));
    mockNoLanguageInRepository();
    qProfileStatusRepository.register(qp1.getQpKey(), REMOVED);
    qProfileStatusRepository.register(qp2.getQpKey(), UNCHANGED);
    qProfileStatusRepository.register(qp3.getQpKey(), UPDATED);
    qProfileStatusRepository.register(qp4.getQpKey(), ADDED);

    when(qualityProfileRuleChangeTextResolver.mapChangeToNumberOfRules(qp3_updated, treeRootHolder.getRoot().getUuid())).thenReturn(CHANGE_TO_NUMBER_OF_RULES_MAP);

    underTest.execute(new TestComputationStepContext());

    assertThat(events).extracting("name").containsOnly(
      "Stop using \"" + QP_NAME_2 + "\" (" + LANGUAGE_KEY_1 + ")",
      "Use \"" + QP_NAME_2 + "\" (" + LANGUAGE_KEY_3 + ")",
      "\"" + QP_NAME_1 + "\" (" + LANGUAGE_KEY_1 + ") updated with " + RULE_CHANGE_TEXT);
  }

  private Language mockLanguageInRepository(String languageKey) {
    Language language = new AbstractLanguage(languageKey, languageKey + "_name") {
      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    };
    when(languageRepository.find(languageKey)).thenReturn(Optional.of(language));
    return language;
  }

  private void mockLanguageNotInRepository(String languageKey) {
    when(languageRepository.find(languageKey)).thenReturn(Optional.empty());
  }

  private void mockNoLanguageInRepository() {
    when(languageRepository.find(anyString())).thenReturn(Optional.empty());
  }

  private void mockQualityProfileMeasures(Component component, @Nullable QualityProfile[] previous, @Nullable QualityProfile[] current) {
    when(measureRepository.getBaseMeasure(component, qualityProfileMetric)).thenReturn(Optional.of(newMeasure(previous)));
    when(measureRepository.getRawMeasure(component, qualityProfileMetric)).thenReturn(Optional.of(newMeasure(current)));
  }

  private static void verifyEvent(Event event, String expectedName, @Nullable String expectedData, @Nullable String expectedDescription) {
    assertThat(event.getName()).isEqualTo(expectedName);
    assertThat(event.getData()).isEqualTo(expectedData);
    assertThat(event.getCategory()).isEqualTo(Event.Category.PROFILE);
    assertThat(event.getDescription()).isEqualTo(expectedDescription);
  }

  private static QualityProfile qp(String qpName, String languageKey, Date date) {
    return new QualityProfile(qpName + "-" + languageKey, qpName, languageKey, date);
  }

  /**
   * Just a trick to use variable args which is shorter than writing new QualityProfile[] { }
   */
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

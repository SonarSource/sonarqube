/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilder;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.qualityprofile.QPMeasureData;
import org.sonar.server.computation.qualityprofile.QualityProfile;
import org.sonar.server.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDateTime;

public class QualityProfileEventsStepTest {

  private static final String QP_NAME_1 = "qp_1";
  private static final String QP_NAME_2 = "qp_2";
  private static final String LANGUAGE_KEY_1 = "language_key1";
  private static final String LANGUAGE_KEY_2 = "language_key_2";
  private static final String LANGUAGE_KEY_3 = "languageKey3";

  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private LanguageRepository languageRepository = mock(LanguageRepository.class);
  private EventRepository eventRepository = mock(EventRepository.class);
  private ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

  private QualityProfileEventsStep underTest = new QualityProfileEventsStep(measureRepository, eventRepository);

  @Test
  public void no_effect_if_no_previous_measure() {
    ComputationContext context = newNoChildRootContext();

    when(measureRepository.findPrevious(context.getRoot(), CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.<MeasureDto>absent());

    underTest.execute(context);

    verifyNoMoreInteractions(eventRepository);
  }

  @Test(expected = IllegalStateException.class)
  public void ISE_if_no_current_measure() {
    ComputationContext context = newNoChildRootContext();

    when(measureRepository.findPrevious(context.getRoot(), CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newMeasureDto()));
    when(measureRepository.findCurrent(context.getRoot(), CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.<BatchReport.Measure>absent());

    underTest.execute(context);
  }

  @Test
  public void no_event_if_no_qp_now_nor_before() {
    ComputationContext context = newNoChildRootContext();

    mockMeasures(context.getRoot(), null, null);

    underTest.execute(context);

    verifyNoMoreInteractions(eventRepository);
  }

  @Test
  public void added_event_if_one_new_qp() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    ComputationContext context = newNoChildRootContext();

    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);
    mockMeasures(context.getRoot(), null, arrayOf(qp));

    underTest.execute(context);

    verify(eventRepository).add(eq(context.getRoot()), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Use '" + qp.getQpName() + "' (" + language.getName() + ")", null);
  }

  @Test
  public void added_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    ComputationContext context = newNoChildRootContext();

    mockLanguageNotInRepository(LANGUAGE_KEY_1);
    mockMeasures(context.getRoot(), null, arrayOf(qp));

    underTest.execute(context);

    verify(eventRepository).add(eq(context.getRoot()), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Use '" + qp.getQpName() + "' (" + qp.getLanguageKey() + ")", null);
  }

  @Test
  public void no_more_used_event_if_qp_no_more_listed() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    ComputationContext context = newNoChildRootContext();

    mockMeasures(context.getRoot(), arrayOf(qp), null);
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    underTest.execute(context);

    verify(eventRepository).add(eq(context.getRoot()), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Stop using '" + qp.getQpName() + "' (" + language.getName() + ")", null);
  }

  @Test
  public void no_more_used_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    ComputationContext context = newNoChildRootContext();

    mockMeasures(context.getRoot(), arrayOf(qp), null);
    mockLanguageNotInRepository(LANGUAGE_KEY_1);

    underTest.execute(context);

    verify(eventRepository).add(eq(context.getRoot()), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(), "Stop using '" + qp.getQpName() + "' (" + qp.getLanguageKey() + ")", null);
  }

  @Test
  public void no_event_if_same_qp_with_same_date() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    ComputationContext context = newNoChildRootContext();

    mockMeasures(context.getRoot(), arrayOf(qp), arrayOf(qp));

    underTest.execute(context);

    verify(eventRepository, never()).add(any(Component.class), any(Event.class));
  }

  @Test
  public void changed_event_if_same_qp_but_no_same_date() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:13+0100"));
    QualityProfile qp2 = qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:17+0100"));
    ComputationContext context = newNoChildRootContext();

    mockMeasures(context.getRoot(), arrayOf(qp1), arrayOf(qp2));
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    underTest.execute(context);

    verify(eventRepository).add(eq(context.getRoot()), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);
    verifyEvent(eventArgumentCaptor.getValue(),
      "Changes in '" + qp2.getQpName() + "' (" + language.getName() + ")",
      "from=" + UtcDateUtils.formatDateTime(parseDateTime("2011-04-25T01:05:14+0100")) + ";key=" + qp1.getQpKey() + ";to="
        + UtcDateUtils.formatDateTime(parseDateTime("2011-04-25T01:05:18+0100")));
  }

  @Test
  public void verify_detection_with_complex_mix_of_qps() {
    ComputationContext context = newNoChildRootContext();
    final Set<Event> events = new HashSet<>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        events.add((Event) invocationOnMock.getArguments()[1]);
        return null;
      }
    }).when(eventRepository).add(eq(context.getRoot()), any(Event.class));

    mockMeasures(
      context.getRoot(), arrayOf(
        qp(QP_NAME_2, LANGUAGE_KEY_1),
        qp(QP_NAME_2, LANGUAGE_KEY_2),
        qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:13+0100"))
      ),
      arrayOf(
        qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:17+0100")),
        qp(QP_NAME_2, LANGUAGE_KEY_2),
        qp(QP_NAME_2, LANGUAGE_KEY_3)
      ));
    mockNoLanguageInRepository();

    underTest.execute(context);

    assertThat(events).extracting("name").containsOnly(
      "Stop using '" + QP_NAME_2 + "' (" + LANGUAGE_KEY_1 + ")",
      "Use '" + QP_NAME_2 + "' (" + LANGUAGE_KEY_3 + ")",
      "Changes in '" + QP_NAME_1 + "' (" + LANGUAGE_KEY_1 + ")"
      );

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
    when(languageRepository.find(languageKey)).thenReturn(Optional.<Language>absent());
  }

  private void mockNoLanguageInRepository() {
    when(languageRepository.find(anyString())).thenReturn(Optional.<Language>absent());
  }

  private void mockMeasures(Component component, @Nullable QualityProfile[] previous, @Nullable QualityProfile[] current) {
    when(measureRepository.findPrevious(component, CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newMeasureDto(previous)));
    when(measureRepository.findCurrent(component, CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newQPBatchMeasure(current)));
  }

  private static void verifyEvent(Event event, String expectedName, @Nullable String expectedData) {
    assertThat(event.getName()).isEqualTo(expectedName);
    assertThat(event.getData()).isEqualTo(expectedData);
    assertThat(event.getCategory()).isEqualTo(Event.Category.PROFILE);
    assertThat(event.getDescription()).isNull();
  }

  private static QualityProfile qp(String qpName, String languageKey) {
    return qp(qpName, languageKey, new Date());
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

  private static MeasureDto newMeasureDto(@Nullable QualityProfile... qps) {
    return new MeasureDto().setData(toJson(qps));
  }

  private static BatchReport.Measure newQPBatchMeasure(@Nullable QualityProfile... qps) {
    return BatchReport.Measure.newBuilder().setStringValue(toJson(qps)).build();
  }

  private static String toJson(@Nullable QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = qps == null ? Collections.<QualityProfile>emptyList() : Arrays.asList(qps);
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }

  private ComputationContext newNoChildRootContext() {
    return newContext(new ComponentTreeBuilder() {
      @Override
      public Component build(ComputationContext context) {
        return new DumbComponent(context, Component.Type.PROJECT, 1, "uuid", "key");
      }
    });
  }

  private ComputationContext newContext(ComponentTreeBuilder builder) {
    return new ComputationContext(mock(BatchReportReader.class), "COMPONENT_KEY", new Settings(), mock(DbClient.class),
      builder, languageRepository);
  }

}

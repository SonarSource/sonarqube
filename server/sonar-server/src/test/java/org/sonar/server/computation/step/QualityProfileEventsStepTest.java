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
import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.UtcDateUtils;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.ComputationContext;
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

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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

  private QualityProfileEventsStep underTest = new QualityProfileEventsStep();

  @Test
  public void no_effect_if_no_previous_measure() {
    when(measureRepository.findPrevious(CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.<MeasureDto>absent());

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    assertThat(context.getRoot().getEventRepository().getEvents()).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void ISE_if_no_current_measure() {
    when(measureRepository.findPrevious(CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newMeasureDto()));
    when(measureRepository.findCurrent(CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.<BatchReport.Measure>absent());

    underTest.execute(newNoChildRootContext());
  }

  @Test
  public void no_event_if_no_qp_now_nor_before() {
    mockMeasures(null, null);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    assertThat(context.getRoot().getEventRepository().getEvents()).isEmpty();
  }

  @Test
  public void added_event_if_one_new_qp() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    mockMeasures(null, arrayOf(qp));
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    List<Event> events = Lists.newArrayList(context.getRoot().getEventRepository().getEvents());
    assertThat(events).hasSize(1);
    verifyEvent(events.get(0), "Use '" + qp.getQpName() + "' (" + language.getName() + ")", null);
  }

  @Test
  public void added_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    mockMeasures(null, arrayOf(qp));
    mockLanguageNotInRepository(LANGUAGE_KEY_1);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    List<Event> events = Lists.newArrayList(context.getRoot().getEventRepository().getEvents());
    assertThat(events).hasSize(1);
    verifyEvent(events.get(0), "Use '" + qp.getQpName() + "' (" + qp.getLanguageKey() + ")", null);
  }

  @Test
  public void no_more_used_event_if_qp_no_more_listed() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    mockMeasures(arrayOf(qp), null);
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    List<Event> events = Lists.newArrayList(context.getRoot().getEventRepository().getEvents());
    assertThat(events).hasSize(1);
    verifyEvent(events.get(0), "Stop using '" + qp.getQpName() + "' (" + language.getName() + ")", null);
  }

  @Test
  public void no_more_used_event_uses_language_key_in_message_if_language_not_found() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    mockMeasures(arrayOf(qp), null);
    mockLanguageNotInRepository(LANGUAGE_KEY_1);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    List<Event> events = Lists.newArrayList(context.getRoot().getEventRepository().getEvents());
    assertThat(events).hasSize(1);
    verifyEvent(events.get(0), "Stop using '" + qp.getQpName() + "' (" + qp.getLanguageKey() + ")", null);
  }

  @Test
  public void no_event_if_same_qp_with_same_date() {
    QualityProfile qp = qp(QP_NAME_1, LANGUAGE_KEY_1);
    mockMeasures(arrayOf(qp), arrayOf(qp));

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    assertThat(context.getRoot().getEventRepository().getEvents()).isEmpty();
  }

  @Test
  public void changed_event_if_same_qp_but_no_same_date() {
    QualityProfile qp1 = qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:13+0100"));
    QualityProfile qp2 = qp(QP_NAME_1, LANGUAGE_KEY_1, parseDateTime("2011-04-25T01:05:17+0100"));
    mockMeasures(arrayOf(qp1), arrayOf(qp2));
    Language language = mockLanguageInRepository(LANGUAGE_KEY_1);

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    List<Event> events = Lists.newArrayList(context.getRoot().getEventRepository().getEvents());
    assertThat(events).hasSize(1);
    verifyEvent(
      events.get(0),
      "Changes in '" + qp2.getQpName() + "' (" + language.getName() + ")",
      "from=" + UtcDateUtils.formatDateTime(parseDateTime("2011-04-25T01:05:14+0100")) + ";key=" + qp1.getQpKey() + ";to="
        + UtcDateUtils.formatDateTime(parseDateTime("2011-04-25T01:05:18+0100")));
  }

  @Test
  public void verify_detection_with_complex_mix_of_qps() {
    mockMeasures(
      arrayOf(
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

    ComputationContext context = newNoChildRootContext();
    underTest.execute(context);

    assertThat(context.getRoot().getEventRepository().getEvents()).extracting("name").containsOnly(
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

  private void mockMeasures(@Nullable QualityProfile[] previous, @Nullable QualityProfile[] current) {
    when(measureRepository.findPrevious(CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newMeasureDto(previous)));
    when(measureRepository.findCurrent(CoreMetrics.QUALITY_PROFILES)).thenReturn(Optional.of(newQPBatchMeasure(current)));
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
        return new EventAndMeasureRepoComponent(context, Component.Type.PROJECT, 1);
      }
    });
  }

  private ComputationContext newContext(ComponentTreeBuilder builder) {
    return new ComputationContext(mock(BatchReportReader.class), "COMPONENT_KEY", new Settings(), mock(DbClient.class),
      builder, languageRepository);
  }

  private class EventAndMeasureRepoComponent extends DumbComponent {
    private final EventRepository eventRepository = new EventRepository() {
      private final Set<Event> events = new HashSet<>();

      @Override
      public void add(Event event) {
        events.add(event);
      }

      @Override
      public Iterable<Event> getEvents() {
        return events;
      }
    };

    public EventAndMeasureRepoComponent(@Nullable org.sonar.server.computation.context.ComputationContext context,
      Type type, int ref, @Nullable Component... children) {
      super(context, type, ref, null, null, children);
    }

    @Override
    public MeasureRepository getMeasureRepository() {
      return measureRepository;
    }

    @Override
    public EventRepository getEventRepository() {
      return eventRepository;
    }
  }

}

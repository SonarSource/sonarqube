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
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.MeasureRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS;

public class QualityGateEventsStepTest {
  private static final DumbComponent PROJECT_COMPONENT = new DumbComponent(1, Component.Type.PROJECT, new DumbComponent(2, Component.Type.MODULE));
  private static final String INVALID_ALERT_STATUS = "trololo";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

  private EventRepository eventRepository = mock(EventRepository.class);
  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, eventRepository, measureRepository);
  public static final String ALERT_TEXT = "alert text";

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(PROJECT_COMPONENT);
  }

  @Test
  public void no_event_if_no_current_ALERT_STATUS_measure() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.<BatchReport.Measure>absent());

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_current_ALTER_STATUS_measure_is_null() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.of(BatchReport.Measure.newBuilder().build()));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_current_ALTER_STATUS_measure_is_unsupported_value() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.of(BatchReport.Measure.newBuilder().setAlertStatus(INVALID_ALERT_STATUS).build()));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_no_past_ALTER_STATUS_and_current_is_OK() {
    String alertStatus = "OK";

    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(alertStatus, null));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.<MeasureDto>absent());

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_no_past_ALTER_STATUS_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS("WARN", "Orange", null);
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS("ERROR", "Red", null);
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_no_alertStatus_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS("ERROR", "Red", new MeasureDto());
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_no_alertStatus_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS("WARN", "Orange", new MeasureDto());
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_invalid_alertStatus_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS("ERROR", "Red", new MeasureDto().setAlertStatus(INVALID_ALERT_STATUS));
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_invalid_alertStatus_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS("WARN", "Orange", new MeasureDto().setAlertStatus(INVALID_ALERT_STATUS));
  }

  private void verify_event_created_if_no_past_ALTER_STATUS(String currentAlterStatus, String expectedEventName, @Nullable MeasureDto measureDto) {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(currentAlterStatus, ALERT_TEXT));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.fromNullable(measureDto));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedEventName);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();
  }

  @Test
  public void no_event_created_if_past_ALTER_STATUS_but_status_is_the_same() {
    String alertStatus = "OK";

    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(alertStatus, ALERT_TEXT));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.of(new MeasureDto().setAlertStatus(alertStatus)));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_exists_and_status_has_changed() {
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("OK", "WARN", "Orange (was Green)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("OK", "ERROR", "Red (was Green)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("WARN", "OK", "Green (was Orange)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("WARN", "ERROR", "Red (was Orange)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("ERROR", "OK", "Green (was Red)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed("ERROR", "WARN", "Orange (was Red)");
  }

  private void verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(String previousAlterStatus, String newAlertStatus, String expectedEventName) {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(newAlertStatus, ALERT_TEXT));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.of(new MeasureDto().setAlertStatus(previousAlterStatus)));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedEventName);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();

    reset(measureRepository, eventRepository);
  }

  private static Optional<BatchReport.Measure> createBatchReportMeasure(String alertStatus, @Nullable String alertText) {
    BatchReport.Measure.Builder builder = BatchReport.Measure.newBuilder().setAlertStatus(alertStatus);
    if (alertText != null) {
      builder.setAlertText(alertText);
    }
    return Optional.of(builder.build());
  }
}

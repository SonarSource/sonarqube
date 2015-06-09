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
import org.sonar.api.notifications.Notification;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.notification.NotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS;
import static org.sonar.server.computation.component.DumbComponent.DUMB_PROJECT;

public class QualityGateEventsStepTest {

  static final String INVALID_STATUS = "trololo";
  static final String DESCRIPTION = "gate errors";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

  EventRepository eventRepository = mock(EventRepository.class);
  MeasureRepository measureRepository = mock(MeasureRepository.class);
  NotificationManager notificationManager = mock(NotificationManager.class);
  QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, eventRepository, measureRepository, notificationManager);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(DUMB_PROJECT);
  }

  @Test
  public void no_event_if_no_status_measure() {
    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.<BatchReport.Measure>absent());

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository, notificationManager);
  }

  @Test
  public void no_event_created_if_status_measure_is_null() {
    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.of(BatchReport.Measure.newBuilder().build()));

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository, notificationManager);
  }

  @Test
  public void no_event_created_if_status_measure_has_unsupported_value() {
    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.of(BatchReport.Measure.newBuilder().setAlertStatus(INVALID_STATUS).build()));

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository, notificationManager);
  }

  @Test
  public void no_event_created_if_OK_and_no_base_status() {
    String alertStatus = "OK";

    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(alertStatus, null));
    when(measureRepository.findPrevious(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.<MeasureDto>absent());

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verify(measureRepository).findPrevious(DUMB_PROJECT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository, notificationManager);
  }

  @Test
  public void event_created_if_WARN_and_no_base_status() {
    verify_event_created_if_no_base_status("WARN", "Orange", null);
  }

  @Test
  public void event_created_if_ERROR_and_no_base_status() {
    verify_event_created_if_no_base_status("ERROR", "Red", null);
  }

  @Test
  public void event_created_if_ERROR_and_base_measure_has_no_status() {
    verify_event_created_if_no_base_status("ERROR", "Red", new MeasureDto());
  }

  @Test
  public void event_created_if_WARN_and_base_measure_has_no_status() {
    verify_event_created_if_no_base_status("WARN", "Orange", new MeasureDto());
  }

  @Test
  public void event_created_if_ERROR_and_base_status_has_invalid_value() {
    verify_event_created_if_no_base_status("ERROR", "Red", new MeasureDto().setAlertStatus(INVALID_STATUS));
  }

  @Test
  public void event_created_if_WARN_and_base_status_has_invalid_value() {
    verify_event_created_if_no_base_status("WARN", "Orange", new MeasureDto().setAlertStatus(INVALID_STATUS));
  }

  private void verify_event_created_if_no_base_status(String status, String expectedLabel, @Nullable MeasureDto measureDto) {
    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(status, DESCRIPTION));
    when(measureRepository.findPrevious(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.fromNullable(measureDto));

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verify(measureRepository).findPrevious(DUMB_PROJECT, ALERT_STATUS);
    verify(eventRepository).add(eq(DUMB_PROJECT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(event.getData()).isNull();

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(DUMB_PROJECT.getKey());
    assertThat(notification.getFieldValue("projectUuid")).isEqualTo(DUMB_PROJECT.getUuid());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(DUMB_PROJECT.getName());
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(status);
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);
  }

  @Test
  public void no_event_created_if_status_same_as_base() {
    String alertStatus = "OK";

    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(alertStatus, DESCRIPTION));
    when(measureRepository.findPrevious(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.of(new MeasureDto().setAlertStatus(alertStatus)));

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verify(measureRepository).findPrevious(DUMB_PROJECT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository, notificationManager);
  }

  @Test
  public void event_created_if_status_changed() {
    verify_event_created_if_status_changed("OK", "WARN", "Orange (was Green)");
    verify_event_created_if_status_changed("OK", "ERROR", "Red (was Green)");
    verify_event_created_if_status_changed("WARN", "OK", "Green (was Orange)");
    verify_event_created_if_status_changed("WARN", "ERROR", "Red (was Orange)");
    verify_event_created_if_status_changed("ERROR", "OK", "Green (was Red)");
    verify_event_created_if_status_changed("ERROR", "WARN", "Orange (was Red)");
  }

  private void verify_event_created_if_status_changed(String baseStatus, String status, String expectedLabel) {
    when(measureRepository.findCurrent(DUMB_PROJECT, ALERT_STATUS)).thenReturn(createBatchReportMeasure(status, DESCRIPTION));
    when(measureRepository.findPrevious(DUMB_PROJECT, ALERT_STATUS)).thenReturn(Optional.of(new MeasureDto().setAlertStatus(baseStatus)));

    underTest.execute();

    verify(measureRepository).findCurrent(DUMB_PROJECT, ALERT_STATUS);
    verify(measureRepository).findPrevious(DUMB_PROJECT, ALERT_STATUS);
    verify(eventRepository).add(eq(DUMB_PROJECT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(event.getData()).isNull();

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(DUMB_PROJECT.getKey());
    assertThat(notification.getFieldValue("projectUuid")).isEqualTo(DUMB_PROJECT.getUuid());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(DUMB_PROJECT.getName());
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(status);
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);

    reset(measureRepository, eventRepository, notificationManager);
  }

  private static Optional<BatchReport.Measure> createBatchReportMeasure(String status, @Nullable String description) {
    BatchReport.Measure.Builder builder = BatchReport.Measure.newBuilder().setAlertStatus(status);
    if (description != null) {
      builder.setAlertText(description);
    }
    return Optional.of(builder.build());
  }
}

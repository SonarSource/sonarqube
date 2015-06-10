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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.notifications.Notification;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureImpl;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.QualityGateStatus;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.notification.NotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.computation.measure.Measure.Level.ERROR;
import static org.sonar.server.computation.measure.Measure.Level.OK;
import static org.sonar.server.computation.measure.Measure.Level.WARN;

public class QualityGateEventsStepTest {
  private static final DumbComponent PROJECT_COMPONENT = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid 1").setKey("key 1")
    .addChildren(DumbComponent.builder(Component.Type.MODULE, 2).build())
    .build();
  private static final String INVALID_ALERT_STATUS = "trololo";
  private static final String ALERT_TEXT = "alert text";
  private static final QualityGateStatus OK_QUALITY_GATE_STATUS = new QualityGateStatus(OK, ALERT_TEXT);
  private static final QualityGateStatus WARN_QUALITY_GATE_STATUS = new QualityGateStatus(WARN, ALERT_TEXT);
  private static final QualityGateStatus ERROR_QUALITY_GATE_STATUS = new QualityGateStatus(ERROR, ALERT_TEXT);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  private ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

  private Metric alertStatusMetric = mock(Metric.class);

  private MetricRepository metricRepository = mock(MetricRepository.class);
  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private EventRepository eventRepository = mock(EventRepository.class);
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, metricRepository, measureRepository, eventRepository, notificationManager);

  @Before
  public void setUp() throws Exception {
    when(metricRepository.getByKey(ALERT_STATUS_KEY)).thenReturn(alertStatusMetric);
    treeRootHolder.setRoot(PROJECT_COMPONENT);
  }

  @Test
  public void no_event_if_no_raw_ALERT_STATUS_measure() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(Optional.<Measure>absent());

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_raw_ALERT_STATUS_measure_is_null() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue()));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  private static Optional<Measure> of(MeasureImpl measure) {
    return Optional.of((Measure) measure);
  }

  @Test
  public void no_event_created_if_raw_ALERT_STATUS_measure_is_unsupported_value() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.create(INVALID_ALERT_STATUS)));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_no_base_ALERT_STATUS_and_raw_is_OK() {
    QualityGateStatus someQGStatus = new QualityGateStatus(Measure.Level.OK);

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue().setQualityGateStatus(someQGStatus)));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue()));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_no_base_ALERT_STATUS_and_raw_is_WARN() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(WARN, "Orange");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_and_raw_is_ERROR() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(ERROR, "Red");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_no_alertStatus_and_raw_is_ERROR() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(ERROR, "Red");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_no_alertStatus_and_raw_is_WARN() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(WARN, "Orange");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_invalid_alertStatus_and_raw_is_ERROR() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(ERROR, "Red");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_invalid_alertStatus_and_raw_is_WARN() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(WARN, "Orange");
  }

  private void verify_event_created_if_no_base_ALERT_STATUS_measure(Measure.Level rawAlterStatus, String expectedLabel) {
    QualityGateStatus someQGStatus = new QualityGateStatus(rawAlterStatus, ALERT_TEXT);

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue().setQualityGateStatus(someQGStatus)));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue()));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectUuid")).isEqualTo(PROJECT_COMPONENT.getUuid());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(rawAlterStatus.name());
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);
  }

  @Test
  public void no_event_created_if_base_ALERT_STATUS_measure_but_status_is_the_same() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue().setQualityGateStatus(OK_QUALITY_GATE_STATUS)));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue().setQualityGateStatus(OK_QUALITY_GATE_STATUS)));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed() {
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(OK, WARN_QUALITY_GATE_STATUS, "Orange (was Green)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(OK, ERROR_QUALITY_GATE_STATUS, "Red (was Green)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(WARN, OK_QUALITY_GATE_STATUS, "Green (was Orange)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(WARN, ERROR_QUALITY_GATE_STATUS, "Red (was Orange)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(ERROR, OK_QUALITY_GATE_STATUS, "Green (was Red)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(ERROR, WARN_QUALITY_GATE_STATUS, "Orange (was Red)");
  }

  private void verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(Measure.Level previousAlertStatus,
    QualityGateStatus newQualityGateStatus, String expectedLabel) {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(MeasureImpl.createNoValue().setQualityGateStatus(newQualityGateStatus)));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(
      of(MeasureImpl.createNoValue().setQualityGateStatus(new QualityGateStatus(previousAlertStatus))));

    underTest.execute();

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();

    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectUuid")).isEqualTo(PROJECT_COMPONENT.getUuid());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(newQualityGateStatus.getStatus().name());
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);

    reset(measureRepository, eventRepository, notificationManager);
  }

}

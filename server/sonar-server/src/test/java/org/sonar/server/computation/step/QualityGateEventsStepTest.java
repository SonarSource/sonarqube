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
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureImpl;
import org.sonar.server.computation.measure.MeasureRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS;
import static org.sonar.server.computation.measure.Measure.AlertStatus.ERROR;
import static org.sonar.server.computation.measure.Measure.AlertStatus.OK;
import static org.sonar.server.computation.measure.Measure.AlertStatus.WARN;

public class QualityGateEventsStepTest {
  private static final DumbComponent PROJECT_COMPONENT = new DumbComponent(1, Component.Type.PROJECT, new DumbComponent(2, Component.Type.MODULE));
  private static final String INVALID_ALERT_STATUS = "trololo";
  private static final String ALERT_TEXT = "alert text";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  private EventRepository eventRepository = mock(EventRepository.class);
  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, eventRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(PROJECT_COMPONENT);
  }

  @Test
  public void no_event_if_no_current_ALERT_STATUS_measure() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(Optional.<Measure>absent());

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_current_ALTER_STATUS_measure_is_null() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue()));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  private static Optional<Measure> of(MeasureImpl measure) {
    return Optional.of((Measure) measure);
  }

  @Test
  public void no_event_created_if_current_ALTER_STATUS_measure_is_unsupported_value() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.create(INVALID_ALERT_STATUS)));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_no_past_ALTER_STATUS_and_current_is_OK() {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(Measure.AlertStatus.OK)));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue()));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_no_past_ALTER_STATUS_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS(WARN, "Orange");
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS(ERROR, "Red");
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_no_alertStatus_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS(ERROR, "Red");
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_no_alertStatus_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS(WARN, "Orange");
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_invalid_alertStatus_and_current_is_ERROR() {
    verify_event_created_if_no_past_ALTER_STATUS(ERROR, "Red");
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_has_invalid_alertStatus_and_current_is_WARN() {
    verify_event_created_if_no_past_ALTER_STATUS(WARN, "Orange");
  }

  private void verify_event_created_if_no_past_ALTER_STATUS(Measure.AlertStatus currentAlterStatus, String expectedEventName) {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(currentAlterStatus).setAlertText(ALERT_TEXT)));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue()));

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
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(OK).setAlertText(ALERT_TEXT)));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(OK)));

    underTest.execute();

    verify(measureRepository).findCurrent(PROJECT_COMPONENT, ALERT_STATUS);
    verify(measureRepository).findPrevious(PROJECT_COMPONENT, ALERT_STATUS);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_past_ALTER_STATUS_exists_and_status_has_changed() {
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(OK, WARN, "Orange (was Green)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(OK, ERROR, "Red (was Green)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(WARN, OK, "Green (was Orange)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(WARN, ERROR, "Red (was Orange)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(ERROR, OK, "Green (was Red)");
    verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(ERROR, WARN, "Orange (was Red)");
  }

  private void verify_event_created_if_past_ALTER_STATUS_exists_and_status_has_changed(Measure.AlertStatus previousAlterStatus,
    Measure.AlertStatus newAlertStatus, String expectedEventName) {
    when(measureRepository.findCurrent(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(newAlertStatus).setAlertText(ALERT_TEXT)));
    when(measureRepository.findPrevious(PROJECT_COMPONENT, ALERT_STATUS)).thenReturn(of(MeasureImpl.createNoValue().setAlertStatus(previousAlterStatus)));

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

}

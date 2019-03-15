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

import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.notifications.Notification;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.component.BranchType;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.OK;

public class QualityGateEventsStepTest {
  private static final String PROJECT_VERSION = randomAlphabetic(19);
  private static final ReportComponent PROJECT_COMPONENT = ReportComponent.builder(Component.Type.PROJECT, 1)
    .setUuid("uuid 1")
    .setKey("key 1")
    .setProjectVersion(PROJECT_VERSION)
    .setBuildString("V1.9")
    .addChildren(ReportComponent.builder(Component.Type.DIRECTORY, 2).build())
    .build();
  private static final String INVALID_ALERT_STATUS = "trololo";
  private static final String ALERT_TEXT = "alert text";
  private static final QualityGateStatus OK_QUALITY_GATE_STATUS = new QualityGateStatus(OK, ALERT_TEXT);
  private static final QualityGateStatus ERROR_QUALITY_GATE_STATUS = new QualityGateStatus(ERROR, ALERT_TEXT);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
  private ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

  private Metric alertStatusMetric = mock(Metric.class);

  private MetricRepository metricRepository = mock(MetricRepository.class);
  private MeasureRepository measureRepository = mock(MeasureRepository.class);
  private EventRepository eventRepository = mock(EventRepository.class);
  private NotificationService notificationService = mock(NotificationService.class);
  private QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, metricRepository, measureRepository, eventRepository, notificationService,
    analysisMetadataHolder);

  @Before
  public void setUp() {
    when(metricRepository.getByKey(ALERT_STATUS_KEY)).thenReturn(alertStatusMetric);
    analysisMetadataHolder
      .setProject(new Project(PROJECT_COMPONENT.getUuid(), PROJECT_COMPONENT.getDbKey(), PROJECT_COMPONENT.getName(), PROJECT_COMPONENT.getDescription(), emptyList()));
    analysisMetadataHolder.setBranch(mock(Branch.class));
    treeRootHolder.setRoot(PROJECT_COMPONENT);
  }

  @Test
  public void no_event_if_no_raw_ALERT_STATUS_measure() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_raw_ALERT_STATUS_measure_is_null() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  private static Optional<Measure> of(Measure measure) {
    return Optional.of(measure);
  }

  @Test
  public void no_event_created_if_raw_ALERT_STATUS_measure_is_unsupported_value() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().create(INVALID_ALERT_STATUS)));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void no_event_created_if_no_base_ALERT_STATUS_and_raw_is_OK() {
    QualityGateStatus someQGStatus = new QualityGateStatus(Measure.Level.OK);

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(someQGStatus).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_no_alertStatus_and_raw_is_ERROR() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(ERROR, "Red");
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_has_invalid_alertStatus_and_raw_is_ERROR() {
    verify_event_created_if_no_base_ALERT_STATUS_measure(ERROR, "Red");
  }

  private void verify_event_created_if_no_base_ALERT_STATUS_measure(Measure.Level rawAlterStatus, String expectedLabel) {
    QualityGateStatus someQGStatus = new QualityGateStatus(rawAlterStatus, ALERT_TEXT);

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(someQGStatus).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(of(Measure.newMeasureBuilder().createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();

    verify(notificationService).deliver(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("projectVersion")).isEqualTo(PROJECT_COMPONENT.getProjectAttributes().getProjectVersion());
    assertThat(notification.getFieldValue("branch")).isNull();
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(rawAlterStatus.name());
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);
  }

  @Test
  public void no_event_created_if_base_ALERT_STATUS_measure_but_status_is_the_same() {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric))
      .thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(OK_QUALITY_GATE_STATUS).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric))
      .thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(OK_QUALITY_GATE_STATUS).createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verifyNoMoreInteractions(measureRepository, eventRepository);
  }

  @Test
  public void event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed() {
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(OK, ERROR_QUALITY_GATE_STATUS, "Red (was Green)");
    verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(ERROR, OK_QUALITY_GATE_STATUS, "Green (was Red)");
  }

  private void verify_event_created_if_base_ALERT_STATUS_measure_exists_and_status_has_changed(Measure.Level previousAlertStatus,
    QualityGateStatus newQualityGateStatus, String expectedLabel) {
    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric))
      .thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(newQualityGateStatus).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(
      of(Measure.newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(previousAlertStatus)).createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(measureRepository).getRawMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(measureRepository).getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric);
    verify(eventRepository).add(eq(PROJECT_COMPONENT), eventArgumentCaptor.capture());
    verifyNoMoreInteractions(measureRepository, eventRepository);

    Event event = eventArgumentCaptor.getValue();
    assertThat(event.getCategory()).isEqualTo(Event.Category.ALERT);
    assertThat(event.getName()).isEqualTo(expectedLabel);
    assertThat(event.getDescription()).isEqualTo(ALERT_TEXT);
    assertThat(event.getData()).isNull();

    verify(notificationService).deliver(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("projectVersion")).isEqualTo(PROJECT_COMPONENT.getProjectAttributes().getProjectVersion());
    assertThat(notification.getFieldValue("branch")).isNull();
    assertThat(notification.getFieldValue("alertLevel")).isEqualTo(newQualityGateStatus.getStatus().name());
    assertThat(notification.getFieldValue("alertName")).isEqualTo(expectedLabel);

    reset(measureRepository, eventRepository, notificationService);
  }

  @Test
  public void verify_branch_name_is_set_in_notification_when_not_main() {
    String branchName = "feature1";
    analysisMetadataHolder.setBranch(new DefaultBranchImpl(branchName) {
      @Override
      public boolean isMain() {
        return false;
      }
    });

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric))
      .thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(OK_QUALITY_GATE_STATUS).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(
      of(Measure.newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(ERROR)).createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(notificationService).deliver(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("projectVersion")).isEqualTo(PROJECT_COMPONENT.getProjectAttributes().getProjectVersion());
    assertThat(notification.getFieldValue("branch")).isEqualTo(branchName);

    reset(measureRepository, eventRepository, notificationService);
  }

  @Test
  public void verify_branch_name_is_not_set_in_notification_when_main() {
    analysisMetadataHolder.setBranch(new DefaultBranchImpl());

    when(measureRepository.getRawMeasure(PROJECT_COMPONENT, alertStatusMetric))
      .thenReturn(of(Measure.newMeasureBuilder().setQualityGateStatus(OK_QUALITY_GATE_STATUS).createNoValue()));
    when(measureRepository.getBaseMeasure(PROJECT_COMPONENT, alertStatusMetric)).thenReturn(
      of(Measure.newMeasureBuilder().setQualityGateStatus(new QualityGateStatus(ERROR)).createNoValue()));

    underTest.execute(new TestComputationStepContext());

    verify(notificationService).deliver(notificationArgumentCaptor.capture());
    Notification notification = notificationArgumentCaptor.getValue();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue("projectKey")).isEqualTo(PROJECT_COMPONENT.getKey());
    assertThat(notification.getFieldValue("projectName")).isEqualTo(PROJECT_COMPONENT.getName());
    assertThat(notification.getFieldValue("projectVersion")).isEqualTo(PROJECT_COMPONENT.getProjectAttributes().getProjectVersion());
    assertThat(notification.getFieldValue("branch")).isEqualTo(null);

    reset(measureRepository, eventRepository, notificationService);
  }

  @Test
  public void no_alert_on_short_living_branches() {
    Branch shortBranch = mock(Branch.class);
    when(shortBranch.getType()).thenReturn(BranchType.SHORT);
    analysisMetadataHolder.setBranch(shortBranch);
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    MetricRepository metricRepository = mock(MetricRepository.class);
    MeasureRepository measureRepository = mock(MeasureRepository.class);
    EventRepository eventRepository = mock(EventRepository.class);
    NotificationService notificationService = mock(NotificationService.class);

    QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, metricRepository, measureRepository,
      eventRepository, notificationService, analysisMetadataHolder);

    underTest.execute(new TestComputationStepContext());

    verifyZeroInteractions(treeRootHolder, metricRepository, measureRepository, eventRepository, notificationService);
  }

  @Test
  public void no_alert_on_pull_request_branches() {
    Branch shortBranch = mock(Branch.class);
    when(shortBranch.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBranch(shortBranch);
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    MetricRepository metricRepository = mock(MetricRepository.class);
    MeasureRepository measureRepository = mock(MeasureRepository.class);
    EventRepository eventRepository = mock(EventRepository.class);
    NotificationService notificationService = mock(NotificationService.class);

    QualityGateEventsStep underTest = new QualityGateEventsStep(treeRootHolder, metricRepository, measureRepository,
      eventRepository, notificationService, analysisMetadataHolder);

    underTest.execute(new TestComputationStepContext());

    verifyZeroInteractions(treeRootHolder, metricRepository, measureRepository, eventRepository, notificationService);
  }
}

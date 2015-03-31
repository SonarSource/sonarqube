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
package org.sonar.batch.qualitygate;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.ProjectTestBuilder;
import org.sonar.batch.protocol.Constants.EventCategory;
import org.sonar.batch.report.EventCache;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class GenerateQualityGateEventsTest {
  private GenerateQualityGateEvents decorator;
  private DecoratorContext context;
  private QualityGate qualityGate;
  private TimeMachine timeMachine;
  private NotificationManager notificationManager;
  private Project project;
  private EventCache eventCache;

  @Before
  public void setup() {
    context = mock(DecoratorContext.class);
    timeMachine = mock(TimeMachine.class);
    qualityGate = mock(QualityGate.class);
    notificationManager = mock(NotificationManager.class);
    eventCache = mock(EventCache.class);
    decorator = new GenerateQualityGateEvents(qualityGate, timeMachine, notificationManager, eventCache);
    project = new ProjectTestBuilder().build();
    when(context.getResource()).thenReturn(project);
  }

  @Test
  public void shouldDependUponAlertStatus() {
    assertThat(decorator.dependsUponAlertStatus()).isEqualTo(CoreMetrics.ALERT_STATUS);
  }

  @Test
  public void shouldNotDecorateIfNoThresholds() {
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldDecorateIfQualityGateEnabled() {
    when(qualityGate.isEnabled()).thenReturn(true);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldNotDecorateIfNotRootProject() {
    decorator.decorate(File.create("Foo"), context);
    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
  }

  @Test
  public void shouldCreateEventWhenNewErrorAlert() {
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, Metric.Level.ERROR.getColorName(), "desc", EventCategory.ALERT, null);
    verifyNotificationSent("Red", "desc", "ERROR", "true");
  }

  @Test
  public void shouldCreateEventWhenNewWarnAlert() {
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.WARN, "desc"));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, Metric.Level.WARN.getColorName(), "desc", EventCategory.ALERT, null);
    verifyNotificationSent("Orange", "desc", "WARN", "true");
  }

  @Test
  public void shouldCreateEventWhenWarnToError() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.WARN, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, "Red (was Orange)", "desc", EventCategory.ALERT, null);
    verifyNotificationSent("Red (was Orange)", "desc", "ERROR", "false");
  }

  @Test
  public void shouldCreateEventWhenErrorToOk() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.OK, null));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, "Green (was Red)", null, EventCategory.ALERT, null);
    verifyNotificationSent("Green (was Red)", null, "OK", "false");
  }

  @Test
  public void shouldCreateEventWhenOkToError() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.OK, null)));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, "Red (was Green)", "desc", EventCategory.ALERT, null);
    verifyNotificationSent("Red (was Green)", "desc", "ERROR", "true");
  }

  @Test
  public void shouldCreateEventWhenErrorToWarn() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.WARN, "desc"));

    decorator.decorate(project, context);

    verify(eventCache).createEvent(project, "Orange (was Red)", "desc", EventCategory.ALERT, null);
    verifyNotificationSent("Orange (was Red)", "desc", "WARN", "false");
  }

  @Test
  public void shouldNotCreateEventWhenNoAlertStatus() {
    decorator.decorate(project, context);

    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotCreateEventWhenSameLevel() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  @Test
  public void shouldNotCreateEventIfNoMoreAlertStatus() {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(null);

    decorator.decorate(project, context);

    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
    verify(notificationManager, never()).scheduleForSending(any(Notification.class));
  }

  private Measure newAlertStatus(Metric.Level level, String label) {
    Measure measure = new Measure(CoreMetrics.ALERT_STATUS, level);
    measure.setAlertStatus(level);
    measure.setAlertText(label);
    return measure;
  }

  private void verifyNotificationSent(String alertName, String alertText, String alertLevel, String isNewAlert) {
    Notification notification = new Notification("alerts")
      .setDefaultMessage("Alert on " + project.getLongName() + ": " + alertName)
      .setFieldValue("projectName", project.getLongName())
      .setFieldValue("projectKey", project.getKey())
      .setFieldValue("projectId", String.valueOf(project.getId()))
      .setFieldValue("alertName", alertName)
      .setFieldValue("alertText", alertText)
      .setFieldValue("alertLevel", alertLevel)
      .setFieldValue("isNewAlert", isNewAlert);
    verify(notificationManager, times(1)).scheduleForSending(eq(notification));
  }
}

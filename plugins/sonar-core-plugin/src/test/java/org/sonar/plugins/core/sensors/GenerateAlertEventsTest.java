/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.dbunit.dataset.DataSetException;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.ProjectTestBuilder;

import java.util.Arrays;
import java.util.Date;

public class GenerateAlertEventsTest {
  private GenerateAlertEvents decorator;
  private DecoratorContext context;
  private RulesProfile profile;
  private TimeMachine timeMachine;
  private Project project;

  @Before
  public void setup() {
    context = mock(DecoratorContext.class);
    timeMachine = mock(TimeMachine.class);
    profile = mock(RulesProfile.class);
    decorator = new GenerateAlertEvents(profile, timeMachine);
    project = new ProjectTestBuilder().build();
  }

  @Test
  public void doNotDecorateIfNoThresholds() {
    assertThat(decorator.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void shouldDecorateIfThresholds() {
    when(profile.getAlerts()).thenReturn(Arrays.asList(new Alert()));
    assertThat(decorator.shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldCreateEventWhenNewErrorAlert() {
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));
    decorator.decorate(project, context);
    verify(context).createEvent(Metric.Level.ERROR.getColorName(), "desc", Event.CATEGORY_ALERT, null);
  }

  @Test
  public void shouldCreateEventWhenNewWarnAlert() {
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.WARN, "desc"));
    decorator.decorate(project, context);
    verify(context).createEvent(Metric.Level.WARN.getColorName(), "desc", Event.CATEGORY_ALERT, null);
  }

  @Test
  public void shouldCreateEventWhenWarnToError() {
    when(timeMachine.getMeasures((TimeMachineQuery) anyObject())).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.WARN, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(context).createEvent("Red (was Orange)", "desc", Event.CATEGORY_ALERT, null);
  }

  @Test
  public void shouldCreateEventWhenErrorToOk() {
    when(timeMachine.getMeasures((TimeMachineQuery) anyObject())).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.OK, null));

    decorator.decorate(project, context);

    verify(context).createEvent("Green (was Red)", null, Event.CATEGORY_ALERT, null);
  }

  @Test
  public void shouldCreateEventWhenErrorToWarn() {
    when(timeMachine.getMeasures((TimeMachineQuery) anyObject())).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.WARN, "desc"));

    decorator.decorate(project, context);

    verify(context).createEvent("Orange (was Red)", "desc", Event.CATEGORY_ALERT, null);
  }

  @Test
  public void shouldNotCreateEventWhenNoAlertStatus() throws DataSetException {
    decorator.decorate(project, context);

    verify(context, never()).createEvent(anyString(), anyString(), anyString(), (Date) isNull());
  }

  @Test
  public void shouldNotCreateEventWhenSameLevel() throws DataSetException {
    when(timeMachine.getMeasures((TimeMachineQuery) anyObject())).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(newAlertStatus(Metric.Level.ERROR, "desc"));

    decorator.decorate(project, context);

    verify(context, never()).createEvent(anyString(), anyString(), anyString(), (Date) isNull());
  }

  @Test
  public void shouldNotCreateEventIfNoMoreAlertStatus() throws DataSetException {
    when(timeMachine.getMeasures((TimeMachineQuery) anyObject())).thenReturn(Arrays.asList(newAlertStatus(Metric.Level.ERROR, "desc")));
    when(context.getMeasure(CoreMetrics.ALERT_STATUS)).thenReturn(null);

    decorator.decorate(project, context);

    verify(context, never()).createEvent(anyString(), anyString(), anyString(), (Date) isNull());
  }


  private Measure newAlertStatus(Metric.Level level, String label) {
    Measure measure = new Measure(CoreMetrics.ALERT_STATUS, level);
    measure.setAlertStatus(level);
    measure.setAlertText(label);
    return measure;
  }
}

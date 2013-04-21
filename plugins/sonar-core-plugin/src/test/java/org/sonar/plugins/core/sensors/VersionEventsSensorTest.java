/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VersionEventsSensorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldExecuteOnProject() throws Exception {
    assertThat(new VersionEventsSensor().shouldExecuteOnProject(null), is(true));
  }

  @Test
  public void testToString() throws Exception {
    assertThat(new VersionEventsSensor().toString(), is("VersionEventsSensor"));
  }

  @Test
  public void shouldDoNothingIfNoVersion() {
    VersionEventsSensor sensor = new VersionEventsSensor();
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(project.getAnalysisVersion()).thenReturn(null);

    sensor.analyse(project, context);

    verify(context, never()).createEvent(any(Resource.class), anyString(), anyString(), anyString(), any(Date.class));
    verify(context, never()).deleteEvent(any(Event.class));
  }

  @Test
  public void shouldCreateVersionEvent() {
    VersionEventsSensor sensor = new VersionEventsSensor();
    SensorContext context = mock(SensorContext.class);

    Project project = mock(Project.class);
    when(project.getAnalysisVersion()).thenReturn("1.5-SNAPSHOT");

    sensor.analyse(project, context);

    verify(context).createEvent(eq(project), eq("1.5-SNAPSHOT"), (String) isNull(), eq(Event.CATEGORY_VERSION), (Date) isNull());
  }

  @Test
  public void shouldHaveOnlyOneEventByVersion() {
    Event sameVersionEvent = mockVersionEvent("1.5-SNAPSHOT");
    Event otherEvent = mockVersionEvent("1.4");
    Event anotherEvent = mockVersionEvent("1.3-SNAPSHOT");

    VersionEventsSensor sensor = new VersionEventsSensor();
    SensorContext context = mock(SensorContext.class);

    Project project = mock(Project.class);
    when(project.getAnalysisVersion()).thenReturn("1.5-SNAPSHOT");

    when(context.getEvents(project)).thenReturn(Lists.newArrayList(sameVersionEvent, otherEvent, anotherEvent));

    sensor.analyse(project, context);

    verify(context).deleteEvent(sameVersionEvent);
    verify(context).createEvent(eq(project), eq("1.5-SNAPSHOT"), (String) isNull(), eq(Event.CATEGORY_VERSION), (Date) isNull());
  }

  private Event mockVersionEvent(String version) {
    Event event = mock(Event.class);
    when(event.isVersionCategory()).thenReturn(true);
    when(event.getName()).thenReturn(version);
    return event;
  }

}

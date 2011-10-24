/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public class VersionEventsSensorTest {

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
  private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd HH:mm");

  @Test
  public void shouldDoNothingIfNoVersion() {
    VersionEventsSensor sensor = new VersionEventsSensor();
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(project.getAnalysisVersion()).thenReturn(null);

    sensor.analyse(project, context);

    verify(context, never()).createEvent((Resource) anyObject(), anyString(), anyString(), anyString(), (Date) anyObject());
    verify(context, never()).deleteEvent((Event) anyObject());
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

    when(context.getEvents(project)).thenReturn(new ArrayList(Arrays.asList(sameVersionEvent, otherEvent, anotherEvent)));

    sensor.analyse(project, context);

    verify(context).deleteEvent(sameVersionEvent);
    verify(context).createEvent(eq(project), eq("1.5-SNAPSHOT"), (String) isNull(), eq(Event.CATEGORY_VERSION), (Date) isNull());
  }

  @Test
  public void shouldDeleteAssociatedSnapshotWhenReleasing() {
    Event snapshotVersion = mockVersionEvent("1.5-SNAPSHOT");
    Event otherEvent = mockVersionEvent("1.4");

    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(project.getAnalysisVersion()).thenReturn("1.5");
    when(context.getEvents(project)).thenReturn(new ArrayList(Arrays.asList(snapshotVersion, otherEvent)));

    VersionEventsSensor sensor = new VersionEventsSensor();
    sensor.analyse(project, context);

    verify(context).deleteEvent(snapshotVersion);
    verify(context).createEvent(eq(project), eq("1.5"), (String) isNull(), eq(Event.CATEGORY_VERSION), (Date) isNull());
  }

  private Event mockVersionEvent(String version) {
    Event event = mock(Event.class);
    when(event.isVersionCategory()).thenReturn(true);
    when(event.getName()).thenReturn(version);
    return event;
  }

  private Event mockEvent(String name, String yyyyMMdd) throws ParseException {
    Event event = mock(Event.class);
    when(event.isVersionCategory()).thenReturn(false);
    when(event.isLinkedToSnapshot()).thenReturn(false);
    when(event.getName()).thenReturn(name);
    when(event.getDate()).thenReturn(dateFormat.parse(yyyyMMdd));
    return event;
  }
}

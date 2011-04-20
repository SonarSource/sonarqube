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

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProfileEventsSensorTest {
    
  private Project project;
  private SensorContext context;
  
  @Before
  public void prepare() {
    project = mock(Project.class);
    context = mock(SensorContext.class);
  }

  @Test
  public void shouldDoNothingIfNoProfile() throws ParseException {
    ProfileEventsSensor sensor = new ProfileEventsSensor(null, null);

    sensor.analyse(project, context);

    verify(context, never()).createEvent((Resource) anyObject(), anyString(), anyString(), anyString(), (Date) anyObject());
  }

  @Test
  public void shouldDoNothingIfNoProfileChange() throws ParseException {
    RulesProfile profile = mockProfile(1);
    TimeMachine timeMachine = mockTM(project, 22.0, 1.0);//Same profile, same version
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context, never()).createEvent((Resource) anyObject(), anyString(), anyString(), anyString(), (Date) anyObject());
  }
  
  @Test
  public void shouldCreateEventIfProfileChange() throws ParseException {
    RulesProfile profile = mockProfile(1);
    TimeMachine timeMachine = mockTM(project, 21.0, 1.0);//Different profile
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project), eq("Profile V1"), eq("A different quality profile was used"), 
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }
  
  @Test
  public void shouldCreateEventIfProfileVersionChange() throws ParseException {
    RulesProfile profile = mockProfile(2);
    TimeMachine timeMachine = mockTM(project, 22.0, 1.0);//Same profile, different version
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project), eq("Profile V2"), eq("A new version of the quality profile was used"), 
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }
  
  @Test
  public void shouldCreateEventIfFirstAnalysis() throws ParseException {
    RulesProfile profile = mockProfile(2);
    TimeMachine timeMachine = mockTM(project, null, null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project), eq("Profile V2"), eq("A different quality profile was used"), 
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }

  @Test
  public void shouldNotCreateEventIfFirstProfileVersionAndStillV1() throws ParseException {
    RulesProfile profile = mockProfile(1);
    TimeMachine timeMachine = mockTMWithNullVersion(project, 22.0);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context, never()).createEvent((Resource) anyObject(), anyString(), anyString(), anyString(), (Date) anyObject());
  }

  @Test
  public void shouldCreateEventIfFirstProfileVersionAndMoreThanV1() throws ParseException {
    RulesProfile profile = mockProfile(2);
    TimeMachine timeMachine = mockTMWithNullVersion(project, 22.0);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project), eq("Profile V2"), eq("A new version of the quality profile was used"), 
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }

  private RulesProfile mockProfile(int version) {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(22);
    when(profile.getName()).thenReturn("Profile");
    when(profile.getVersion()).thenReturn(version);//New version
    return profile;
  }
  
  private TimeMachine mockTM(Project project, double profileValue, double versionValue) {
    return mockTM(project, new Measure(CoreMetrics.PROFILE, profileValue), 
        new Measure(CoreMetrics.PROFILE_VERSION, versionValue));
  }
  
  private TimeMachine mockTMWithNullVersion(Project project, double profileValue) {
    return mockTM(project, new Measure(CoreMetrics.PROFILE, profileValue), null);
  }
  
  private TimeMachine mockTM(Project project, Measure result1, Measure result2) {
    TimeMachineQuery query = new TimeMachineQuery(project).setOnlyLastAnalysis(true)
        .setMetrics(CoreMetrics.PROFILE, CoreMetrics.PROFILE_VERSION);
    TimeMachine timeMachine = mock(TimeMachine.class);
    
    when(timeMachine.getMeasures(eq(query))).thenReturn(Arrays.<Measure>asList(
        result1,
        result2
    ));
    return timeMachine;
  }

}

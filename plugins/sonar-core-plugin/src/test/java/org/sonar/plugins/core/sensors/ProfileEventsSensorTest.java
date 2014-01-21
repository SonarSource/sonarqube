/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.sensors;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.batch.RulesProfileWrapper;
import org.sonar.batch.scan.language.ModuleLanguages;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ProfileEventsSensorTest {

  private Project project;
  private SensorContext context;
  private ModuleLanguages moduleLanguages;
  private RulesProfileWrapper wrapper;
  private RulesProfile profile;

  @Before
  public void prepare() {
    project = mock(Project.class);
    context = mock(SensorContext.class);

    moduleLanguages = new ModuleLanguages(new Settings(), new Languages(Java.INSTANCE));
    moduleLanguages.addLanguage("java");
    Map<String, RulesProfile> ruleProfilesPerLanguages = new HashMap<String, RulesProfile>();
    profile = mock(RulesProfile.class);
    ruleProfilesPerLanguages.put("java", profile);
    wrapper = new RulesProfileWrapper(moduleLanguages, ruleProfilesPerLanguages);
  }

  @Test
  public void shouldExecuteWhenProfileWithId() {
    when(profile.getId()).thenReturn(123);
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, null, moduleLanguages);

    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    verifyZeroInteractions(project);
  }

  @Test
  public void shouldNotExecuteIfProfileIsNotWrapper() {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, null, moduleLanguages);

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
    verifyZeroInteractions(project);
  }

  @Test
  public void shouldDoNothingIfNoProfileChange() {
    mockProfileWithVersion(1);
    TimeMachine timeMachine = mockTM(22.0, "Foo", 1.0); // Same profile, same version
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, timeMachine, moduleLanguages);

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void shouldCreateEventIfProfileChange() {
    mockProfileWithVersion(1);
    TimeMachine timeMachine = mockTM(21.0, "Bar", 1.0); // Different profile
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, timeMachine, moduleLanguages);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
      eq("Foo version 1"),
      eq("Foo version 1 is used instead of Bar version 1"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileVersionChange() {
    mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(22.0, "Foo", 1.0); // Same profile, different version
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, timeMachine, moduleLanguages);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
      eq("Foo version 2"),
      eq("Foo version 2 is used instead of Foo version 1"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldNotCreateEventIfFirstAnalysis() {
    mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(null, null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, timeMachine, moduleLanguages);

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void shouldCreateEventIfFirstAnalysisWithVersionsAndVersionMoreThan1() {
    mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(22.0, "Foo", null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(wrapper, timeMachine, moduleLanguages);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
      eq("Foo version 2"),
      eq("Foo version 2 is used instead of Foo version 1"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  private void mockProfileWithVersion(int version) {
    when(profile.getId()).thenReturn(22);
    when(profile.getName()).thenReturn("Foo");
    when(profile.getVersion()).thenReturn(version);
  }

  private TimeMachine mockTM(double profileId, String profileName, Double versionValue) {
    return mockTM(new Measure(CoreMetrics.PROFILE, profileId, profileName), versionValue == null ? null : new Measure(CoreMetrics.PROFILE_VERSION, versionValue));
  }

  private TimeMachine mockTM(Measure result1, Measure result2) {
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(result1 == null ? Collections.<Measure>emptyList() : Arrays.asList(result1))
      .thenReturn(result2 == null ? Collections.<Measure>emptyList() : Arrays.asList(result2));

    return timeMachine;
  }
}

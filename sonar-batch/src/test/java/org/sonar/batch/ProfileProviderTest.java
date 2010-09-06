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
package org.sonar.batch;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class ProfileProviderTest {

  @Test
  public void shouldGetProjectProfile() {
    ProfileProvider provider = new ProfileProvider();
    Project project = new Project("project").setLanguageKey(Java.KEY);
    Project module = new Project("module").setParent(project).setLanguageKey(Java.KEY);
    ProfilesDao dao = mock(ProfilesDao.class);

    when(dao.getActiveProfile(Java.KEY, "project")).thenReturn(newProfile());

    assertNotNull(provider.provide(module, dao));

    verify(dao, never()).getActiveProfile(Java.KEY, "module");
    verify(dao).getActiveProfile(Java.KEY, "project");
  }

  private RulesProfile newProfile() {
    RulesProfile profile = new RulesProfile();
    profile.setAlerts(Collections.<Alert>emptyList());
    profile.setActiveRules(Collections.<ActiveRule>emptyList());
    return profile;
  }

  @Test
  public void mavenPropertyShouldOverrideProfile() {
    ProfileProvider provider = new ProfileProvider();
    ProfilesDao dao = mock(ProfilesDao.class);
    Project project = new Project("project").setLanguageKey(Java.KEY);

    MapConfiguration conf = new MapConfiguration(new HashMap());
    conf.addProperty(ProfileProvider.PARAM_PROFILE, "profile1");
    project.setConfiguration(conf);

    when(dao.getProfile(Java.KEY, "profile1")).thenReturn(newProfile());

    provider.provide(project, dao);

    verify(dao).getProfile(Java.KEY, "profile1");
    verify(dao, never()).getActiveProfile(Java.KEY, "project");
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailIfProfileIsNotFound() {
    ProfileProvider provider = new ProfileProvider();
    Project project = new Project("project").setLanguageKey(Java.KEY);
    ProfilesDao dao = mock(ProfilesDao.class);

    MapConfiguration conf = new MapConfiguration(new HashMap());
    conf.addProperty(ProfileProvider.PARAM_PROFILE, "unknown");
    project.setConfiguration(conf);


    when(dao.getProfile(Java.KEY, "profile1")).thenReturn(null);

    provider.provide(project, dao);
  }

}

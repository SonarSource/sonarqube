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
package org.sonar.batch;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;

import org.sonar.batch.DefaultProfileLoader;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.batch.ProfileLoader;
import org.sonar.jpa.dao.ProfilesDao;

public class DefaultProfileLoaderTest {

  private ProfilesDao dao;
  private ProfileLoader loader;

  @Before
  public void setUp() {
    dao = mock(ProfilesDao.class);
    loader = new DefaultProfileLoader(dao);
  }

  @Test
  public void shouldGetProjectProfile() {
    Project project = new Project("project").setLanguageKey(Java.KEY);
    Project module = new Project("module").setParent(project).setLanguageKey(Java.KEY);

    when(dao.getActiveProfile(Java.KEY, "project")).thenReturn(newProfile());

    assertNotNull(loader.load(module));

    verify(dao, never()).getActiveProfile(Java.KEY, "module");
    verify(dao).getActiveProfile(Java.KEY, "project");
  }

  private RulesProfile newProfile() {
    RulesProfile profile = new RulesProfile();
    profile.setAlerts(Collections.<Alert> emptyList());
    profile.setActiveRules(Collections.<ActiveRule> emptyList());
    return profile;
  }

  @Test
  public void mavenPropertyShouldOverrideProfile() {
    Project project = new Project("project").setLanguageKey(Java.KEY);

    MapConfiguration conf = new MapConfiguration(new HashMap());
    conf.addProperty(DefaultProfileLoader.PARAM_PROFILE, "profile1");
    project.setConfiguration(conf);

    when(dao.getProfile(Java.KEY, "profile1")).thenReturn(newProfile());

    loader.load(project);

    verify(dao).getProfile(Java.KEY, "profile1");
    verify(dao, never()).getActiveProfile(Java.KEY, "project");
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailIfProfileIsNotFound() {
    Project project = new Project("project").setLanguageKey(Java.KEY);

    MapConfiguration conf = new MapConfiguration(new HashMap());
    conf.addProperty(DefaultProfileLoader.PARAM_PROFILE, "unknown");
    project.setConfiguration(conf);

    when(dao.getProfile(Java.KEY, "profile1")).thenReturn(null);

    loader.load(project);
  }

}

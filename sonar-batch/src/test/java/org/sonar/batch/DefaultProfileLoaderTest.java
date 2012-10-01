/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.dao.ProfilesDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProfileLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ProfilesDao dao;
  private Project javaProject = new Project("project").setLanguageKey(Java.KEY);

  @Before
  public void setUp() {
    dao = mock(ProfilesDao.class);
  }

  @Test
  public void should_get_configured_profile() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "legacy profile");
    when(dao.getProfile(Java.KEY, "legacy profile")).thenReturn(RulesProfile.create("legacy profile", "java"));

    RulesProfile profile = new DefaultProfileLoader(dao, settings).load(javaProject);

    assertThat(profile.getName()).isEqualTo("legacy profile");
  }

  @Test
  public void should_fail_if_not_found() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "unknown");

    thrown.expect(SonarException.class);
    thrown.expectMessage("Quality profile not found : unknown, language java");
    new DefaultProfileLoader(dao, settings).load(javaProject);
  }

  /**
   * SONAR-3125
   */
  @Test
  public void should_give_explicit_message_if_default_profile_not_found() {
    Project cobolProject = new Project("cobol-javaProject").setLanguageKey("cobol");

    thrown.expect(SonarException.class);
    thrown.expectMessage("You must install a plugin that supports the language 'cobol'");
    new DefaultProfileLoader(dao, new Settings()).load(cobolProject);
  }

}

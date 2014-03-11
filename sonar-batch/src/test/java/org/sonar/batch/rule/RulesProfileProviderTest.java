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
package org.sonar.batch.rule;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.jpa.dao.ProfilesDao;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RulesProfileProviderTest {

  ModuleQProfiles qProfiles = mock(ModuleQProfiles.class);
  Settings settings = new Settings();
  ProfilesDao dao = mock(ProfilesDao.class);
  RulesProfileProvider provider = new RulesProfileProvider();

  @Test
  public void merge_profiles() throws Exception {
    ModuleQProfiles.QProfile qProfile = new ModuleQProfiles.QProfile(33, "Sonar way", "java", 12);
    when(qProfiles.findAll()).thenReturn(Arrays.asList(qProfile));
    RulesProfile hibernateProfile = new RulesProfile("Sonar way", "java");
    when(dao.getProfile("java", "Sonar way")).thenReturn(hibernateProfile);

    RulesProfile profile = provider.provide(qProfiles, settings, dao);

    // merge of all profiles
    assertThat(profile).isNotNull().isInstanceOf(RulesProfileWrapper.class);
    assertThat(profile.getLanguage()).isEqualTo("");
    assertThat(profile.getName()).isEqualTo("SonarQube");
    assertThat(profile.getAlerts()).isEmpty();
    assertThat(profile.getActiveRules()).isEmpty();
    try {
      profile.getId();
      fail();
    } catch (IllegalStateException e) {
      // id must not be used at all
    }
  }

  @Test
  public void keep_compatibility_with_single_language_projects() throws Exception {
    settings.setProperty("sonar.language", "java");

    ModuleQProfiles.QProfile qProfile = new ModuleQProfiles.QProfile(33, "Sonar way", "java", 12);
    when(qProfiles.findByLanguage("java")).thenReturn(qProfile);
    RulesProfile hibernateProfile = new RulesProfile("Sonar way", "java").setVersion(12);
    when(dao.getProfile("java", "Sonar way")).thenReturn(hibernateProfile);

    RulesProfile profile = provider.provide(qProfiles, settings, dao);

    // no merge, directly the old hibernate profile
    assertThat(profile).isNotNull();
    assertThat(profile.getLanguage()).isEqualTo("java");
    assertThat(profile.getName()).isEqualTo("Sonar way");
    assertThat(profile.getVersion()).isEqualTo(12);
  }
}

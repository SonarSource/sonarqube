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

package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RubyQProfilesServiceTest {

  @Mock
  QProfiles qProfiles;

  RubyQProfilesService service;

  @Before
  public void setUp() throws Exception {
    service = new RubyQProfilesService(qProfiles);
  }

  @Test
  public void search_profiles() throws Exception {
    when(qProfiles.searchProfiles()).thenReturn(newArrayList(
      new QProfile().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    ));

    assertThat(service.searchProfiles()).hasSize(1);
  }

  @Test
  public void new_profile() throws Exception {
    service.newProfile(ImmutableMap.<String, Object>of(
      "name", "Default",
      "language", "java")
    );
    verify(qProfiles).newProfile(eq("Default"), eq("java"), any(UserSession.class));
  }
}

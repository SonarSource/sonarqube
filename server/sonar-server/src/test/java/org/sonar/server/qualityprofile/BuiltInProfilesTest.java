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

package org.sonar.server.qualityprofile;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BuiltInProfilesTest {

  BuiltInProfiles cache;

  @Before
  public void setUp() throws Exception {
    cache = new BuiltInProfiles();
  }

  @Test
  public void add_profiles() throws Exception {
    cache.put("java", "Default");
    cache.put("java", "Sonar Way");
    cache.put("js", "Default");

    assertThat(cache.byLanguage("java")).containsOnly("Default", "Sonar Way");
    assertThat(cache.byLanguage("js")).containsOnly("Default");
  }

  @Test
  public void not_add_same_profile_name() throws Exception {
    cache.put("java", "Default");
    cache.put("java", "Default");

    assertThat(cache.byLanguage("java")).containsOnly("Default");
  }
}

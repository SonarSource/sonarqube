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
package org.sonar.jpa.dao;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class ProfilesDaoTest extends AbstractDbUnitTestCase {

  private ProfilesDao profilesDao;

  @Before
  public void setup() {
    profilesDao = new ProfilesDao(getSession());
  }

  @Test
  public void should_get_profile_by_name() {
    RulesProfile profile = RulesProfile.create("my profile", "java");
    getSession().save(profile);

    assertThat(profilesDao.getProfile("unknown language", "my profile")).isNull();
    assertThat(profilesDao.getProfile("java", "my profile").getName()).isEqualTo("my profile");
  }
}

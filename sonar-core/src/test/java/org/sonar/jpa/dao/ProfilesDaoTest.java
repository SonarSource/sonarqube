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
package org.sonar.jpa.dao;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ProfilesDaoTest extends AbstractDbUnitTestCase {

  private ProfilesDao profilesDao;

  @Before
  public void setup() {
    profilesDao = new ProfilesDao(getSession());
  }

  @Test
  public void shouldGetProfiles() {
    setupData("shouldGetProfiles");

    List<RulesProfile> profiles = profilesDao.getProfiles("java");

    assertThat(profiles.size(), is(2));
  }

  @Test
  public void testGetActiveProfile() {
    RulesProfile testDefaultProfile = new RulesProfile("default", "java", true, true);
    RulesProfile testProfile = new RulesProfile("not default", "java", false, false);
    getSession().save(testDefaultProfile, testProfile);

    ResourceModel testResourceWithProfile = new ResourceModel(ResourceModel.SCOPE_PROJECT, "withProfile", "qual", null, "test");
    testResourceWithProfile.setRulesProfile(testProfile);
    ResourceModel testResourceWithNoProfile = new ResourceModel(ResourceModel.SCOPE_PROJECT, "withoutProfile", "qual", null, "test");
    getSession().save(testResourceWithProfile, testResourceWithNoProfile);

    assertNull(profilesDao.getActiveProfile("wrongLanguage", "withoutProfile"));
    assertEquals(testDefaultProfile.getId(), profilesDao.getActiveProfile("java", "wrongKey").getId());
    assertEquals(testDefaultProfile.getId(), profilesDao.getActiveProfile("java", "withoutProfile").getId());
    assertEquals(testProfile.getId(), profilesDao.getActiveProfile("java", "withProfile").getId());
  }

}

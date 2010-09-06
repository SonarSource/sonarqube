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
package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;

import static org.junit.Assert.*;

public class ProfilesManagerTest extends AbstractDbUnitTestCase {

  private ProfilesManager manager;

  @Before
  public void setup() {
    manager = new ProfilesManager(getSession(), null, null, null);
  }

  @Test
  public void testDeleteProfile() {
    RulesProfile testDefaultProfile = new RulesProfile("default", "java", true, true);
    RulesProfile testProfile = new RulesProfile("not default", "java", false, false);
    ResourceModel testResourceWithProfile = new ResourceModel(ResourceModel.SCOPE_PROJECT, "withProfile", "qual", null, "test");
    testResourceWithProfile.setRulesProfile(testProfile);
    getSession().save(testDefaultProfile, testProfile, testResourceWithProfile);

    getSession().commit();
    getSession().getEntityManager().clear();

    manager.deleteProfile(testDefaultProfile.getId());
    // default profiles cannot be deleted
    assertNotNull(getSession().getEntity(RulesProfile.class, testDefaultProfile.getId()));

    manager.deleteProfile(testProfile.getId());
    // default profiles cannot be deleted
    assertNull(getSession().getEntity(RulesProfile.class, testProfile.getId()));

    testResourceWithProfile = getSession().getEntity(ResourceModel.class, testResourceWithProfile.getId());
    assertNull(testResourceWithProfile.getRulesProfile());

  }

  @Test
  public void testDeleteAllProfiles() {

    RulesProfile test1 = new RulesProfile("test1", "java", true, true);
    RulesProfile test2 = new RulesProfile("test2", "java", false, false);

    ResourceModel testResourceWithProfile = new ResourceModel(ResourceModel.SCOPE_PROJECT, "withProfile", "qual", null, "test");
    testResourceWithProfile.setRulesProfile(test1);
    getSession().save(test1, test2, testResourceWithProfile);

    assertEquals(new Long(2), getHQLCount(RulesProfile.class));

    manager.deleteAllProfiles();

    assertEquals(new Long(0), getHQLCount(RulesProfile.class));
  }
}

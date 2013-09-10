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
package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.core.dryrun.DryRunCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class InheritedProfilesTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null, mock(DryRunCache.class));
  }

  @Test
  public void shouldCheckCycles() {
    setupData("shouldCheckCycles");
    RulesProfile level1 = profilesManager.getProfile("java", "level1");
    RulesProfile level2 = profilesManager.getProfile("java", "level2");
    RulesProfile level3 = profilesManager.getProfile("java", "level3");

    assertThat(profilesManager.getParentProfile(level1), nullValue());
    assertThat(profilesManager.getParentProfile(level2), is(level1));
    assertThat(profilesManager.getParentProfile(level3), is(level2));

    assertThat(profilesManager.isCycle(level1, level1), is(true));
    assertThat(profilesManager.isCycle(level1, level3), is(true));
    assertThat(profilesManager.isCycle(level1, level2), is(true));
    assertThat(profilesManager.isCycle(level2, level3), is(true));
  }

  @Test
  public void shouldSetParent() {
    setupData("shouldSetParent");
    profilesManager.changeParentProfile(2, "parent", "admin");
    checkTables("shouldSetParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldChangeParent() {
    setupData("shouldChangeParent");
    profilesManager.changeParentProfile(3, "new_parent", "admin");
    checkTables("shouldChangeParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldRemoveParent() {
    setupData("shouldRemoveParent");
    profilesManager.changeParentProfile(2, null, "admin");
    checkTables("shouldRemoveParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldDeactivateInChildren() {
    setupData("shouldDeactivateInChildren");
    profilesManager.deactivated(1, 1, "admin");
    checkTables("shouldDeactivateInChildren", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldActivateInChildren() {
    setupData("shouldActivateInChildren");
    profilesManager.activated(1, 1, "admin");
    checkTables("shouldActivateInChildren", "active_rules", "rules_profiles", "active_rule_parameters");
  }

}

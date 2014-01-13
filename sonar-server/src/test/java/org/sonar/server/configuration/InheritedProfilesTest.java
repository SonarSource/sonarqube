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
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InheritedProfilesTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null, mock(PreviewCache.class));
  }

  @Test
  public void shouldSetParent() {
    setupData("shouldSetParent");
    profilesManager.profileParentChanged(2, "parent", "admin");
    checkTables("shouldSetParent", "active_rules");
  }

  @Test
  public void shouldChangeParent() {
    setupData("shouldChangeParent");
    profilesManager.profileParentChanged(3, "new_parent", "admin");
    checkTables("shouldChangeParent", "active_rules");
  }

  @Test
  public void shouldRemoveParent() {
    setupData("shouldRemoveParent");
    profilesManager.profileParentChanged(2, null, "admin");
    checkTables("shouldRemoveParent", "active_rules");
  }

  @Test
  public void shouldDeactivateInChildren() {
    setupData("shouldDeactivateInChildren");
    ProfilesManager.RuleInheritanceActions actions = profilesManager.deactivated(1, 1, "admin");
    checkTables("shouldDeactivateInChildren", "active_rules", "rules_profiles");
    assertThat(actions.idsToIndex()).containsOnly(1);
    assertThat(actions.idsToDelete()).containsOnly(2);
  }

  @Test
  public void shouldNotDeactivateOverridingChildren() {
    setupData("shouldNotDeactivateOverridingChildren");
    ProfilesManager.RuleInheritanceActions actions = profilesManager.deactivated(1, 1, "admin");
    checkTables("shouldNotDeactivateOverridingChildren", "active_rules", "rules_profiles");
    assertThat(actions.idsToIndex()).containsOnly(1, 2);
    assertThat(actions.idsToDelete()).isEmpty();
  }

  @Test
  public void shouldActivateInChildren() {
    setupData("shouldActivateInChildren");
    ProfilesManager.RuleInheritanceActions actions = profilesManager.activated(1, 1, "admin");
    checkTables("shouldActivateInChildren", "active_rules", "rules_profiles", "active_rule_parameters");
    assertThat(actions.idsToIndex()).containsOnly(1, 2);
    assertThat(actions.idsToDelete()).isEmpty();
  }

}

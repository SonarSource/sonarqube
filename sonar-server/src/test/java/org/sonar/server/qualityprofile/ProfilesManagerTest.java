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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.server.qualityprofile.ProfilesManager;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProfilesManagerTest extends AbstractDbUnitTestCase {

  private ProfilesManager manager;

  @Before
  public void before() {
    manager = new ProfilesManager(getSession(), null, mock(PreviewCache.class));
  }

  @Test
  public void delete_all_profiles() {
    RulesProfile test1 = RulesProfile.create("test1", "java");
    test1.setDefaultProfile(true);
    RulesProfile test2 = RulesProfile.create("test2", "java");

    getSession().save(test1, test2);

    assertThat(getHQLCount(RulesProfile.class)).isEqualTo(2);

    manager.deleteAllProfiles();

    assertThat(getHQLCount(RulesProfile.class)).isEqualTo(0);
  }

  @Test
  public void remove_activated_rules() {
    Rule rule1 = Rule.create("repo", "key");

    RulesProfile profile1 = RulesProfile.create("profile1", "xoo");
    ActiveRule activeRule1 = new ActiveRule(profile1, rule1, RulePriority.BLOCKER);

    RulesProfile profile2 = RulesProfile.create("profile2", "foo");
    ActiveRule activeRule2 = new ActiveRule(profile2, rule1, RulePriority.BLOCKER);

    getSession().save(profile1, rule1, activeRule1, profile2, activeRule2);

    manager.removeActivatedRules(rule1.getId());

    assertThat(getHQLCount(ActiveRule.class)).isEqualTo(0);
  }

}

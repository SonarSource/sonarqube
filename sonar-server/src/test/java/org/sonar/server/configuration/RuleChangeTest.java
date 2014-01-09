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
import org.sonar.api.rules.ActiveRuleChange;
import org.sonar.api.rules.ActiveRuleParamChange;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleChangeTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null, mock(PreviewCache.class));
  }

  @Test
  public void should_increase_version_if_used() {
    setupData("initialData");
    profilesManager.activated(2, 3, "admin");
    checkTables("versionIncreaseIfUsed", "rules_profiles");
  }

  @Test
  public void should_increase_version_if_used_and_in_children() {
    setupData("initialData");
    profilesManager.activated(1, 1, "admin");
    checkTables("versionIncreaseIfUsedAndInChildren", "rules_profiles");
  }

  @Test
  public void should_track_rule_activation() {
    setupData("initialData");
    profilesManager.activated(2, 3, "admin");
    checkTables("ruleActivated", new String[] {"change_date"}, "active_rule_changes");
  }

  @Test
  public void should_track_rule_deactivation() {
    setupData("initialData");
    profilesManager.deactivated(2, 3, "admin");
    checkTables("ruleDeactivated", new String[] {"change_date"}, "active_rule_changes");
  }

  @Test
  public void should_track_rule_param_change() {
    setupData("initialData");
    profilesManager.ruleParamChanged(2, 3, "param1", "20", "30", "admin");
    checkTables("ruleParamChanged", new String[] {"change_date"}, "active_rule_changes", "active_rule_param_changes");
  }

  @Test
  public void should_not_track_rule_param_change_if_no_change() {
    setupData("initialData");
    profilesManager.ruleParamChanged(2, 3, "param1", "20", "20", "admin");
    assertThat(getHQLCount(ActiveRuleChange.class)).isEqualTo(0);
  }

  @Test
  public void should_track_rule_severity_change() {
    setupData("initialData");
    profilesManager.ruleSeverityChanged(2, 3, RulePriority.BLOCKER, RulePriority.CRITICAL, "admin");
    checkTables("ruleSeverityChanged", new String[] {"change_date"}, "active_rule_changes");
  }

  @Test
  public void should_not_track_rule_severity_change_if_no_change() {
    setupData("initialData");
    profilesManager.ruleSeverityChanged(2, 3, RulePriority.BLOCKER, RulePriority.BLOCKER, "admin");
    assertThat(getHQLCount(ActiveRuleChange.class)).isEqualTo(0);
  }

  @Test
  public void should_track_rule_revert() {
    setupData("ruleReverted");
    profilesManager.revert(2, 3, "admin");
    checkTables("ruleReverted", new String[] {"change_date"}, "active_rule_changes", "active_rule_param_changes");
  }

  @Test
  public void should_not_track_param_change_on_rule_revert_if_no_param() {
    setupData("should_not_track_param_change_on_rule_revert_if_no_param");
    profilesManager.revert(2, 3, "admin");
    assertThat(getHQLCount(ActiveRuleParamChange.class)).isEqualTo(0);
  }

  @Test
  public void should_not_track_param_change_on_rule_revert_if_no_change() {
    setupData("should_not_track_param_change_on_rule_revert_if_no_param");
    profilesManager.revert(2, 3, "admin");
    assertThat(getHQLCount(ActiveRuleParamChange.class)).isEqualTo(0);
  }

  @Test
  public void should_track_change_parent_profile() {
    setupData("changeParentProfile");
    profilesManager.changeParentProfile(2, "parent", "admin");
    checkTables("changeParentProfile", new String[] {"change_date"}, "active_rule_changes");
  }

  @Test
  public void should_track_remove_activated_rules() {
    setupData("initialData");
    Rule rule = getSession().reattach(Rule.class, 1);
    profilesManager.removeActivatedRules(rule.getId());
    checkTables("removeActivatedRules", new String[] {"change_date"}, "active_rule_changes", "active_rules");
  }

}

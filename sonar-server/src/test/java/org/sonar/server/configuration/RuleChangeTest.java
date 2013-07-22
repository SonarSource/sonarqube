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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class RuleChangeTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null);
  }

  @Test
  public void testVersionIncreaseIfUsed() {
    setupData("initialData");
    profilesManager.activated(2, 3, "admin");
    checkTables("versionIncreaseIfUsed", "rules_profiles");
  }

  @Test
  public void testVersionIncreaseIfUsedAndInChildren() {
    setupData("initialData");
    profilesManager.activated(1, 1, "admin");
    checkTables("versionIncreaseIfUsedAndInChildren", "rules_profiles");
  }

  @Test
  public void testRuleActivated() {
    setupData("initialData");
    profilesManager.activated(2, 3, "admin");
    checkTables("ruleActivated", new String[]{"change_date"}, "active_rule_changes");
  }

  @Test
  public void testRuleDeactivated() {
    setupData("initialData");
    profilesManager.deactivated(2, 3, "admin");
    checkTables("ruleDeactivated", new String[]{"change_date"}, "active_rule_changes");
  }

  @Test
  public void testRuleParamChanged() {
    setupData("initialData");
    profilesManager.ruleParamChanged(2, 3, "param1", "20", "30", "admin");
    checkTables("ruleParamChanged", new String[]{"change_date"}, "active_rule_changes", "active_rule_param_changes");
  }

  @Test
  public void testRuleSeverityChanged() {
    setupData("initialData");
    profilesManager.ruleSeverityChanged(2, 3, RulePriority.BLOCKER, RulePriority.CRITICAL, "admin");
    checkTables("ruleSeverityChanged", new String[]{"change_date"}, "active_rule_changes");
  }

  @Test
  public void testRuleReverted() {
    setupData("ruleReverted");
    profilesManager.revert(2, 3, "admin");
    checkTables("ruleReverted", new String[]{"change_date"}, "active_rule_changes", "active_rule_param_changes");
  }

  @Test
  public void testChangeParentProfile() {
    setupData("changeParentProfile");
    profilesManager.changeParentProfile(2, "parent", "admin");
    checkTables("changeParentProfile", new String[]{"change_date"}, "active_rule_changes");
  }

  @Test
  public void testRemoveActivatedRules() {
    setupData("initialData");
    Rule rule = getSession().reattach(Rule.class, 1);
    profilesManager.removeActivatedRules(rule);
    checkTables("removeActivatedRules", new String[]{"change_date"}, "active_rule_changes", "active_rules");
  }


}

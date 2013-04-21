/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.profiles;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class RulesProfileTest {

  @Test
  public void searchRulesByConfigKey() {
    RulesProfile profile = RulesProfile.create();
    profile.activateRule(Rule.create("repo", "key1", "name1"), null);
    profile.activateRule(Rule.create("repo", "key2", "name2").setConfigKey("config2"), null);

    assertNull(profile.getActiveRuleByConfigKey("repo", "unknown"));
    assertThat(profile.getActiveRuleByConfigKey("repo", "config2").getRuleKey(), is("key2"));
  }

  @Test
  public void activateRuleWithDefaultPriority() {
    RulesProfile profile = RulesProfile.create();
    Rule rule = Rule.create("repo", "key1", "name1").setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, null);
    assertThat(profile.getActiveRule("repo", "key1").getSeverity(), is(RulePriority.CRITICAL));
  }

  @Test
  public void activateRuleWithSpecificPriority() {
    RulesProfile profile = RulesProfile.create();
    Rule rule = Rule.create("repo", "key1", "name1").setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, RulePriority.MINOR);
    assertThat(profile.getActiveRule("repo", "key1").getSeverity(), is(RulePriority.MINOR));
  }

  @Test
  public void defaultVersionIs1() {
    RulesProfile profile = RulesProfile.create();    
    assertThat(profile.getVersion(), is(1));
  }
}

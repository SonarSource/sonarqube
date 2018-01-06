/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.profiles;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


public class RulesProfileTest {

  @Test
  public void searchRulesByConfigKey() {
    RulesProfile profile = RulesProfile.create();
    profile.activateRule(Rule.create("repo", "key1", "name1"), null);
    profile.activateRule(Rule.create("repo", "key2", "name2").setConfigKey("config2"), null);

    assertThat(profile.getActiveRuleByConfigKey("repo", "unknown")).isNull();
    assertThat(profile.getActiveRuleByConfigKey("repo", "config2").getRuleKey()).isEqualTo("key2");
  }

  @Test
  public void activateRuleWithDefaultPriority() {
    RulesProfile profile = RulesProfile.create();
    Rule rule = Rule.create("repo", "key1", "name1").setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, null);
    assertThat(profile.getActiveRule("repo", "key1").getSeverity()).isEqualTo(RulePriority.CRITICAL);
  }

  @Test
  public void activateRuleWithSpecificPriority() {
    RulesProfile profile = RulesProfile.create();
    Rule rule = Rule.create("repo", "key1", "name1").setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, RulePriority.MINOR);
    assertThat(profile.getActiveRule("repo", "key1").getSeverity()).isEqualTo(RulePriority.MINOR);
  }

  @Test
  public void fail_to_activate_already_activated_rule() {
    RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("repo", "key1", "name1").setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, null);

    try {
      profile.activateRule(rule, null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class)
        .hasMessage("The definition of the profile 'Default' (language 'java') contains multiple occurrences of the 'repo:key1' rule. The plugin which declares this profile should fix this.");
    }
  }
}

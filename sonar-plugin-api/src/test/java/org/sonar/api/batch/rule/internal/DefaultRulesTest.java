/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.rule.internal;

import org.sonar.api.rule.RuleKey;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRulesTest {
  @Test
  public void testRepeatedInternalKey() {
    List<NewRule> newRules = new LinkedList<>();
    newRules.add(createRule("key1", "repo", "internal"));
    newRules.add(createRule("key2", "repo", "internal"));
    
    DefaultRules rules = new DefaultRules(newRules);
    assertThat(rules.findByInternalKey("repo", "internal")).hasSize(2);
    assertThat(rules.find(RuleKey.of("repo", "key1"))).isNotNull();
    assertThat(rules.find(RuleKey.of("repo", "key2"))).isNotNull();
    assertThat(rules.findByRepository("repo")).hasSize(2);
  }
  
  @Test
  public void testNonExistingKey() {
    List<NewRule> newRules = new LinkedList<>();
    newRules.add(createRule("key1", "repo", "internal"));
    newRules.add(createRule("key2", "repo", "internal"));
    
    DefaultRules rules = new DefaultRules(newRules);
    assertThat(rules.findByInternalKey("xx", "xx")).hasSize(0);
    assertThat(rules.find(RuleKey.of("xxx", "xx"))).isNull();
    assertThat(rules.findByRepository("xxxx")).hasSize(0);
  }
  
  @Test
  public void testRepeatedRule() {
    List<NewRule> newRules = new LinkedList<>();
    newRules.add(createRule("key", "repo", "internal"));
    newRules.add(createRule("key", "repo", "internal"));
    
    DefaultRules rules = new DefaultRules(newRules);
    assertThat(rules.find(RuleKey.of("repo", "key"))).isNotNull();
  }
  
  private NewRule createRule(String key, String repo, String internalKey) {
    RuleKey ruleKey = RuleKey.of(repo, key);
    NewRule newRule = new NewRule(ruleKey);
    newRule.setInternalKey(internalKey);
    
    return newRule;
  }
}

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
package org.sonar.scanner.rule;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.batch.rule.Rules;
import org.sonarqube.ws.Rules.ListResponse.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RulesProviderTest {
  @Test
  public void testRuleTranslation() {
    RulesLoader loader = mock(RulesLoader.class);
    when(loader.load()).thenReturn(Lists.newArrayList(getTestRule()));

    RulesProvider provider = new RulesProvider();

    Rules rules = provider.provide(loader);

    assertThat(rules.findAll()).hasSize(1);
    assertRule(rules.findAll().iterator().next());
  }

  private static void assertRule(org.sonar.api.batch.rule.Rule r) {
    Rule testRule = getTestRule();

    assertThat(r.name()).isEqualTo(testRule.getName());
    assertThat(r.internalKey()).isEqualTo(testRule.getInternalKey());
    assertThat(r.key().rule()).isEqualTo(testRule.getKey());
    assertThat(r.key().repository()).isEqualTo(testRule.getRepository());
  }

  private static Rule getTestRule() {
    Rule.Builder ruleBuilder = Rule.newBuilder();
    ruleBuilder.setKey("key1");
    ruleBuilder.setRepository("repo1");
    ruleBuilder.setName("name");
    ruleBuilder.setInternalKey("key1");
    return ruleBuilder.build();

  }
}

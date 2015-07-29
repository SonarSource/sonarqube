/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.rule;

import org.assertj.core.api.Condition;

import org.sonar.api.batch.rule.Rules;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.sonar.batch.protocol.input.Rule;
import org.sonar.batch.protocol.input.RulesSearchResult;
import org.junit.Test;

public class RulesProviderTest {
  @Test
  public void testRuleTranslation() {
    final Rule testRule = new Rule("repo1:key1", "repo1", "key1", "name");

    RulesSearchResult loadResult = new RulesSearchResult();
    loadResult.setRules(Arrays.asList(testRule));
    RulesLoader loader = mock(RulesLoader.class);
    when(loader.load()).thenReturn(loadResult);

    RulesProvider provider = new RulesProvider();
    Rules rules = provider.provide(loader);

    assertThat(rules.findAll()).hasSize(1);
    assertThat(rules.findAll()).are(new Condition<org.sonar.api.batch.rule.Rule>() {

      @Override
      public boolean matches(org.sonar.api.batch.rule.Rule value) {
        return value.key().rule().equals(testRule.internalKey()) &&
          value.internalKey().equals(testRule.internalKey()) &&
          value.name().equals(testRule.name()) &&
          value.key().repository().equals(testRule.repositoryKey());
      }
    });
  }
}

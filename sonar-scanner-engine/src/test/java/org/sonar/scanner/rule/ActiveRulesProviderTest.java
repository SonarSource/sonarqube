/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRule;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.bootstrap.ScannerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.PROJECT_KEY_PROPERTY;

public class ActiveRulesProviderTest {
  public static final String PROJECT_KEY = "my-awesome-project";
  private final ActiveRulesProvider provider = new ActiveRulesProvider();
  private final DefaultActiveRulesLoader loader = mock(DefaultActiveRulesLoader.class);

  @Test
  public void testCombinationOfRules() {
    LoadedActiveRule r1 = mockRule("rule1");
    LoadedActiveRule r2 = mockRule("rule2");
    LoadedActiveRule r3 = mockRule("rule3");

    r1.setImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH));

    when(loader.load(PROJECT_KEY)).thenReturn(List.of(r1, r2, r3));

    HashMap<String, String> propertiesMap = new HashMap<>();
    propertiesMap.put(PROJECT_KEY_PROPERTY, PROJECT_KEY);
    ScannerProperties scannerProperties = new ScannerProperties(propertiesMap);
    DefaultActiveRules activeRules = provider.provide(loader, scannerProperties);

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findAll()).extracting("ruleKey").containsOnly(
      RuleKey.of("rule1", "rule1"), RuleKey.of("rule2", "rule2"), RuleKey.of("rule3", "rule3"));

    verify(loader).load(PROJECT_KEY);

    Map<String, ActiveRule> activeRuleByKey = activeRules.findAll().stream().collect(Collectors.toMap(e -> e.ruleKey().rule(), e -> e));
    assertThat(((DefaultActiveRule) activeRuleByKey.get("rule1")).impacts())
      .containsExactlyInAnyOrderEntriesOf(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH));

    assertThat(((DefaultActiveRule) activeRuleByKey.get("rule2")).impacts()).isEmpty();
    assertThat(((DefaultActiveRule) activeRuleByKey.get("rule3")).impacts()).isEmpty();

    assertThat(activeRules.getDeprecatedRuleKeys(RuleKey.of("rule1", "rule1"))).containsOnly("rule1old:rule1old");
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testParamsAreTransformed() {
    LoadedActiveRule r1 = mockRule("rule1");
    LoadedActiveRule r2 = mockRule("rule2", b -> b.setParams(Map.of("foo1", "bar1", "foo2", "bar2")));

    when(loader.load(PROJECT_KEY)).thenReturn(List.of(r1, r2));

    HashMap<String, String> propertiesMap = new HashMap<>();
    propertiesMap.put(PROJECT_KEY_PROPERTY, PROJECT_KEY);
    ScannerProperties scannerProperties = new ScannerProperties(propertiesMap);
    ActiveRules activeRules = provider.provide(loader, scannerProperties);

    assertThat(activeRules.findAll()).hasSize(2);
    assertThat(activeRules.findAll()).extracting("ruleKey", "params").containsOnly(
      Tuple.tuple(RuleKey.of("rule1", "rule1"), ImmutableMap.of()),
      Tuple.tuple(RuleKey.of("rule2", "rule2"), ImmutableMap.of("foo1", "bar1", "foo2", "bar2")));

    verify(loader).load(PROJECT_KEY);
    verifyNoMoreInteractions(loader);
  }

  @SafeVarargs
  private static LoadedActiveRule mockRule(String name, Consumer<LoadedActiveRule>... consumers) {
    LoadedActiveRule rule = new LoadedActiveRule();
    rule.setName(name);
    rule.setRuleKey(RuleKey.of(name, name));
    rule.setDeprecatedKeys(ImmutableSet.of(RuleKey.of(name + "old", name + "old")));
    for (Consumer<LoadedActiveRule> consumer : consumers) {
      consumer.accept(rule);
    }
    return rule;
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultActiveRulesTest {
  private final RuleKey ruleKey = RuleKey.of("repo", "rule");

  @Test
  public void empty_returns_nothing() {
    DefaultActiveRules underTest = new DefaultActiveRules(Collections.emptyList());

    assertThat(underTest.getDeprecatedRuleKeys(ruleKey)).isEmpty();
    assertThat(underTest.matchesDeprecatedKeys(ruleKey, WildcardPattern.create("**"))).isFalse();
  }

  @Test
  public void finds_match() {
    DefaultActiveRules underTest = new DefaultActiveRules(ImmutableSet.of(new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setDeprecatedKeys(singleton(RuleKey.of("oldrepo", "oldrule")))
      .build()));

    assertThat(underTest.getDeprecatedRuleKeys(ruleKey)).containsOnly("oldrepo:oldrule");
    assertThat(underTest.matchesDeprecatedKeys(ruleKey, WildcardPattern.create("oldrepo:oldrule"))).isTrue();
  }

  @Test
  public void finds_match_with_multiple_deprecated_keys() {
    DefaultActiveRules underTest = new DefaultActiveRules(ImmutableSet.of(new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setDeprecatedKeys(ImmutableSet.of(RuleKey.of("oldrepo", "oldrule"), (RuleKey.of("oldrepo2", "oldrule2"))))
      .build()));

    assertThat(underTest.getDeprecatedRuleKeys(ruleKey)).containsOnly("oldrepo:oldrule", "oldrepo2:oldrule2");
    assertThat(underTest.matchesDeprecatedKeys(ruleKey, WildcardPattern.create("oldrepo:oldrule"))).isTrue();
  }
}

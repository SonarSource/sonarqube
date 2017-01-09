/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.qualityprofile;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ActiveRuleKeyTest {

  @Test
  public void of() {
    RuleKey ruleKey = RuleKey.of("xoo", "R1");
    ActiveRuleKey key = ActiveRuleKey.of("P1", ruleKey);
    assertThat(key.qProfile()).isEqualTo("P1");
    assertThat(key.ruleKey()).isSameAs(ruleKey);
    assertThat(key.toString()).isEqualTo("P1:xoo:R1");
  }

  @Test
  public void rule_key_can_contain_colons() {
    RuleKey ruleKey = RuleKey.of("squid", "Key:With:Some::Colons");
    ActiveRuleKey key = ActiveRuleKey.of("P1", ruleKey);
    assertThat(key.qProfile()).isEqualTo("P1");
    assertThat(key.ruleKey()).isSameAs(ruleKey);
    assertThat(key.toString()).isEqualTo("P1:squid:Key:With:Some::Colons");
  }

  @Test
  public void profile_must_not_be_null() {
    try {
      ActiveRuleKey.of(null, RuleKey.of("xoo", "R1"));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("QProfile is missing");
    }
  }

  @Test
  public void rule_key_must_not_be_null() {
    try {
      ActiveRuleKey.of("P1", null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("RuleKey is missing");
    }
  }

  @Test
  public void parse() {
    ActiveRuleKey key = ActiveRuleKey.parse("P1:xoo:R1");
    assertThat(key.qProfile()).isEqualTo("P1");
    assertThat(key.ruleKey().repository()).isEqualTo("xoo");
    assertThat(key.ruleKey().rule()).isEqualTo("R1");
  }

  @Test
  public void parse_fail_when_less_than_three_colons() {
    try {
      ActiveRuleKey.parse("P1:xoo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Bad format of activeRule key: P1:xoo");
    }
  }

  @Test
  public void equals_and_hashcode() {
    ActiveRuleKey key1 = ActiveRuleKey.parse("P1:xoo:R1");
    ActiveRuleKey key1b = ActiveRuleKey.parse("P1:xoo:R1");
    ActiveRuleKey key2 = ActiveRuleKey.parse("P1:xoo:R2");
    ActiveRuleKey key3 = ActiveRuleKey.parse("P2:xoo:R1");
    assertThat(key1.equals(key1)).isTrue();
    assertThat(key1.equals(key1b)).isTrue();
    assertThat(key1.equals(null)).isFalse();
    assertThat(key1.equals("P1:xoo:R1")).isFalse();
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.equals(key3)).isFalse();

    assertThat(key1.hashCode()).isEqualTo(key1.hashCode());
  }
}

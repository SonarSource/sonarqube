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
package org.sonar.api.rule;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleKeyTest {

  @Test
  public void testOf() throws Exception {
    RuleKey key = RuleKey.of("squid", "NullDeref");
    assertThat(key.repository()).isEqualTo("squid");
    assertThat(key.rule()).isEqualTo("NullDeref");
  }

  @Test
  public void key_can_contain_colons() {
    RuleKey key = RuleKey.of("squid", "Key:With:Some::Colons");
    assertThat(key.repository()).isEqualTo("squid");
    assertThat(key.rule()).isEqualTo("Key:With:Some::Colons");
  }

  @Test
  public void repository_must_not_be_null() {
    try {
      RuleKey.of(null, "NullDeref");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Repository must be set");
    }
  }

  @Test
  public void repository_must_not_be_empty() {
    try {
      RuleKey.of("", "NullDeref");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Repository must be set");
    }
  }

  @Test
  public void rule_must_not_be_null() {
    try {
      RuleKey.of("squid", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule must be set");
    }
  }

  @Test
  public void rule_must_not_be_empty() {
    try {
      RuleKey.of("squid", "");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule must be set");
    }
  }

  @Test
  public void encode_and_decode_string() {
    RuleKey key = RuleKey.of("squid", "NullDeref");
    String serialized = key.toString();
    assertThat(serialized).isEqualTo("squid:NullDeref");
    RuleKey parsed = RuleKey.parse(serialized);
    assertThat(parsed.repository()).isEqualTo("squid");
    assertThat(parsed.rule()).isEqualTo("NullDeref");
    assertThat(parsed.toString()).isEqualTo("squid:NullDeref");
  }

  @Test
  public void parse_key_with_colons() {
    RuleKey key = RuleKey.parse("squid:Key:With:Some::Colons");
    assertThat(key.repository()).isEqualTo("squid");
    assertThat(key.rule()).isEqualTo("Key:With:Some::Colons");
  }

  @Test
  public void not_accept_bad_format() {
    try {
      RuleKey.parse("foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Invalid rule key: foo");
    }
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    RuleKey key1 = RuleKey.of("squid", "NullDeref");
    RuleKey key2 = RuleKey.of("squid", "NullDeref");
    RuleKey key3 = RuleKey.of("squid", "Other");

    assertThat(key1).isEqualTo(key1);
    assertThat(key1).isEqualTo(key2);
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key1).isNotEqualTo(null);
    assertThat(key1.hashCode()).isEqualTo(key1.hashCode());
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  public void test_compareTo() {
    RuleKey aa = RuleKey.of("A", "A");
    RuleKey ab = RuleKey.of("A", "B");

    assertThat(ab).isGreaterThan(aa);
    assertThat(aa).isLessThan(ab);
    assertThat(aa).isNotEqualTo(ab);
    assertThat(ab).isNotEqualTo(aa);
    assertThat(aa).isEqualTo(aa);
    assertThat(ab).isEqualTo(ab);
  }
}

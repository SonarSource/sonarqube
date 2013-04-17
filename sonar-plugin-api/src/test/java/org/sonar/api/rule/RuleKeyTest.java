/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rule;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleKeyTest {
  @Test
  public void testOf() throws Exception {
    RuleKey key = RuleKey.of("squid", "NullDeref");
    assertThat(key.repository()).isEqualTo("squid");
    assertThat(key.rule()).isEqualTo("NullDeref");
  }

  @Test
  public void repository_must_not_be_null() throws Exception {
    try {
      RuleKey.of(null, "NullDeref");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Repository must be set");
    }
  }

  @Test
  public void repository_must_not_be_empty() throws Exception {
    try {
      RuleKey.of("", "NullDeref");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Repository must be set");
    }
  }

  @Test
  public void rule_must_not_be_null() throws Exception {
    try {
      RuleKey.of("squid", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule must be set");
    }
  }

  @Test
  public void rule_must_not_be_empty() throws Exception {
    try {
      RuleKey.of("squid", "");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule must be set");
    }
  }

  @Test
  public void should_encode_and_decode_string() throws Exception {
    RuleKey key = RuleKey.of("squid", "NullDeref");
    String serialized = key.toString();
    assertThat(serialized).isEqualTo("squid:NullDeref");
    RuleKey parsed = RuleKey.parse(serialized);
    assertThat(parsed.repository()).isEqualTo("squid");
    assertThat(parsed.rule()).isEqualTo("NullDeref");
    assertThat(parsed.toString()).isEqualTo("squid:NullDeref");
  }

  @Test
  public void should_not_accept_bad_format() throws Exception {
    try {
      RuleKey.parse("foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Bad format of rule key: foo");
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
}

/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.test;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.sonar.test.MoreConditions.reflectionEqualTo;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.test.MoreConditions.contains;
import static org.sonar.test.MoreConditions.equalsIgnoreEOL;

public class MoreConditionsTest {
  @Test
  public void should_compare_equals_texts() {
    assertThat("TEXT").satisfies(equalsIgnoreEOL("TEXT"));
  }

  @Test
  public void should_ignore_line_feeds_and_carriage_returns() {
    assertThat("BEFORE\nAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFORE\rAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFORE\n\rAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFOREAFTER").satisfies(equalsIgnoreEOL("BEFORE\n\rAFTER"));
  }

  @Test
  public void should_refuse_different_values() {
    assertThat("TEXT").doesNotSatisfy(equalsIgnoreEOL("DIFFERENT"));
  }

  @Test
  public void should_accept_empty_values() {
    assertThat("").satisfies(equalsIgnoreEOL(""));
    assertThat("").satisfies(equalsIgnoreEOL("\n\r"));
    assertThat("\n\r").satisfies(equalsIgnoreEOL(""));
  }

  @Test
  public void should_find_value_in_collection() {
    Collection<String> collection = Arrays.asList("ONE", "TWO");

    assertThat(collection).satisfies(contains("ONE"));
    assertThat(collection).satisfies(contains("TWO"));
    assertThat(collection).doesNotSatisfy(contains("THREE"));
  }

  @Test
  public void should_find_value_in_collection_using_reflection() {
    Collection<Bean> collection = Arrays.asList(
        new Bean("key1", "value1"),
        null,
        new Bean("key2", "value2"));

    assertThat(collection).satisfies(contains(new Bean("key1", "value1")));
    assertThat(collection).satisfies(contains(new Bean("key2", "value2")));
    assertThat(collection).doesNotSatisfy(contains(new Bean("key1", "value2")));
    assertThat(collection).doesNotSatisfy(contains(new Bean("key2", "value1")));
    assertThat(collection).doesNotSatisfy(contains(new Bean("", "")));
  }

  @Test
  public void should_compare_using_reflection() {
    Bean bean1 = new Bean("key1", "value1");
    Bean bean2 = new Bean("key2", "value2");

    assertThat(bean1).is(reflectionEqualTo(bean1));
    assertThat(bean2).is(reflectionEqualTo(bean2));
    assertThat(bean1).isNot(reflectionEqualTo(bean2));
    assertThat(bean2).isNot(reflectionEqualTo(bean1));
    assertThat(bean1).isNot(reflectionEqualTo(null));
    assertThat(bean2).isNot(reflectionEqualTo(null));
    assertThat(bean1).isNot(reflectionEqualTo(new Object()));
    assertThat(bean2).isNot(reflectionEqualTo(new Object()));
    assertThat(bean1).isNot(reflectionEqualTo(new Bean("key1", "value2")));
    assertThat(bean1).isNot(reflectionEqualTo(new Bean("key2", "value1")));
    assertThat(bean1).isNot(reflectionEqualTo(new Bean("", "")));
  }

  static final class Bean {
    final String key;
    final String value;

    Bean(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }
}

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
package org.sonar.api.server.rule;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleParamTypeTest {

  @Test
  public void testEquals() throws Exception {
    RuleParamType noOptions = RuleParamType.INTEGER;
    RuleParamType withOptions1 = RuleParamType.singleListOfValues("one", "two");
    RuleParamType withOptions2 = RuleParamType.singleListOfValues("three", "four");

    assertThat(RuleParamType.INTEGER)
      .isEqualTo(RuleParamType.INTEGER)
      .isNotEqualTo(RuleParamType.STRING)
      .isNotEqualTo("INTEGER")
      .isNotEqualTo(withOptions1)
      .isNotEqualTo(null);

    assertThat(withOptions1)
      .isEqualTo(withOptions1)
      .isNotEqualTo(noOptions)
      .isNotEqualTo(withOptions2)
      .isNotEqualTo("SINGLE_SELECT_LIST,values=one,two,")
      .isNotEqualTo(null);
  }

  @Test
  public void testHashCode() throws Exception {
    assertThat(RuleParamType.INTEGER.hashCode()).isEqualTo(RuleParamType.INTEGER.hashCode());
  }

  @Test
  public void testInteger() throws Exception {
    RuleParamType type = RuleParamType.INTEGER;
    assertThat(type.toString()).isEqualTo("INTEGER");
    assertThat(RuleParamType.parse(type.toString()).type()).isEqualTo("INTEGER");
    assertThat(RuleParamType.parse(type.toString()).values()).isEmpty();
    assertThat(RuleParamType.parse(type.toString()).toString()).isEqualTo("INTEGER");
  }

  @Test
  public void testListOfValues() throws Exception {
    RuleParamType selectList = RuleParamType.parse("SINGLE_SELECT_LIST,values=\"foo,bar\",");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "bar");
    assertThat(selectList.multiple()).isFalse();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,values=\"foo,bar,\"");

    RuleParamType.parse("SINGLE_SELECT_LIST,values=\"foo,bar\",multiple=false");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "bar");
    assertThat(selectList.multiple()).isFalse();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,values=\"foo,bar,\"");

    RuleParamType.parse("SINGLE_SELECT_LIST,\"values=foo,bar\",\"multiple=false\"");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "bar");
    assertThat(selectList.multiple()).isFalse();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,values=\"foo,bar,\"");

    // escape values
    selectList = RuleParamType.singleListOfValues("foo", "one,two|three,four");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "one,two|three,four");
    assertThat(selectList.multiple()).isFalse();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,values=\"foo,\"\"one,two|three,four\"\",\"");
  }

  @Test
  public void testMultipleListOfValues() throws Exception {
    RuleParamType selectList = RuleParamType.parse("SINGLE_SELECT_LIST,values=\"foo,bar\",multiple=true");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "bar");
    assertThat(selectList.multiple()).isTrue();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,multiple=true,values=\"foo,bar,\"");

    RuleParamType.parse("SINGLE_SELECT_LIST,\"values=foo,bar\",\"multiple=true\"");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "bar");
    assertThat(selectList.multiple()).isTrue();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,multiple=true,values=\"foo,bar,\"");

    // escape values
    selectList = RuleParamType.multipleListOfValues("foo", "one,two|three,four");
    assertThat(selectList.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(selectList.values()).containsOnly("foo", "one,two|three,four");
    assertThat(selectList.multiple()).isTrue();
    assertThat(selectList.toString()).isEqualTo("SINGLE_SELECT_LIST,multiple=true,values=\"foo,\"\"one,two|three,four\"\",\"");
  }

  @Test
  public void support_deprecated_formats() {
    assertThat(RuleParamType.parse("b")).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(RuleParamType.parse("i")).isEqualTo(RuleParamType.INTEGER);
    assertThat(RuleParamType.parse("i{}")).isEqualTo(RuleParamType.INTEGER);
    assertThat(RuleParamType.parse("s")).isEqualTo(RuleParamType.STRING);
    assertThat(RuleParamType.parse("s{}")).isEqualTo(RuleParamType.STRING);
    assertThat(RuleParamType.parse("r")).isEqualTo(RuleParamType.STRING);
    assertThat(RuleParamType.parse("TEXT")).isEqualTo(RuleParamType.TEXT);
    assertThat(RuleParamType.parse("STRING")).isEqualTo(RuleParamType.STRING);
    assertThat(RuleParamType.parse("REGULAR_EXPRESSION")).isEqualTo(RuleParamType.STRING);
    RuleParamType list = RuleParamType.parse("s[FOO,BAR]");
    assertThat(list.type()).isEqualTo("SINGLE_SELECT_LIST");
    assertThat(list.values()).containsOnly("FOO", "BAR");
  }
}

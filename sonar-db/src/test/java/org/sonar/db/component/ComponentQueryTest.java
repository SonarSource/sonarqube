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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.PROJECT;

public class ComponentQueryTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ComponentQuery underTest;

  @Test
  public void build_query() throws Exception {
    underTest = ComponentQuery.builder()
      .setNameOrKeyQuery("key")
      .setLanguage("java")
      .setQualifiers(PROJECT)
      .build();

    assertThat(underTest.getNameOrKeyQuery()).isEqualTo("key");
    assertThat(underTest.getLanguage()).isEqualTo("java");
    assertThat(underTest.getQualifiers()).containsOnly(PROJECT);
  }

  @Test
  public void build_query_minimal_properties() throws Exception {
    underTest = ComponentQuery.builder()
      .setQualifiers(PROJECT)
      .build();

    assertThat(underTest.getNameOrKeyQuery()).isNull();
    assertThat(underTest.getLanguage()).isNull();
    assertThat(underTest.getQualifiers()).containsOnly(PROJECT);
  }

  @Test
  public void fail_if_no_qualifier_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one qualifier must be provided");

    underTest = ComponentQuery.builder().setLanguage("java").build();
  }

  @Test
  public void test_getNameOrKeyQueryToSqlForResourceIndex() throws Exception {
    underTest = ComponentQuery.builder()
      .setNameOrKeyQuery("NAME/key")
      .setQualifiers(PROJECT)
      .build();

    assertThat(underTest.getNameOrKeyQueryToSqlForResourceIndex()).isEqualTo("name//key%");
  }

  @Test
  public void test_getNameOrKeyQueryToSqlForProjectKey() throws Exception {
    underTest = ComponentQuery.builder()
      .setNameOrKeyQuery("name/key")
      .setQualifiers(PROJECT)
      .build();

    assertThat(underTest.getNameOrKeyQueryToSqlForProjectKey()).isEqualTo("name//key%");
  }
}

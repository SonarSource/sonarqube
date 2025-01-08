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
package org.sonar.ce.task.projectexport.rule;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuleTest {
  private static final String SOME_DUMP_UUID = "uuid-12334";
  private static final String SOME_REPOSITORY = "some repository";
  private static final String SOME_KEY = "some key";


  private Rule underTest = new Rule(SOME_DUMP_UUID, SOME_REPOSITORY, SOME_KEY);

  @Test
  public void constructor_throws_NPE_if_repository_is_null() {
    assertThatThrownBy(() -> new Rule(SOME_DUMP_UUID, null, SOME_KEY))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("repository can not be null");
  }

  @Test
  public void constructor_throws_NPE_if_key_is_null() {
    assertThatThrownBy(() -> new Rule(SOME_DUMP_UUID, SOME_REPOSITORY, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key can not be null");
  }

  @Test
  public void equals_compares_repository_and_key() {
    assertThat(underTest).isNotEqualTo(new Rule(SOME_DUMP_UUID, SOME_KEY, SOME_REPOSITORY));
    assertThat(underTest).isNotEqualTo(new Rule(SOME_DUMP_UUID, "other repository", SOME_KEY));
    assertThat(underTest).isNotEqualTo(new Rule(SOME_DUMP_UUID, SOME_REPOSITORY, "other key"));
  }

  @Test
  public void equals_ignores_dump_id() {
    assertThat(underTest).isEqualTo(new Rule("uuid-8888", SOME_REPOSITORY, SOME_KEY));
  }

  @Test
  public void hashcode_is_based_on_repository_and_key() {
    assertThat(underTest).hasSameHashCodeAs(new Rule(SOME_DUMP_UUID, SOME_REPOSITORY, SOME_KEY));
    assertThat(underTest.hashCode()).isNotEqualTo(new Rule(SOME_DUMP_UUID, SOME_KEY, SOME_REPOSITORY).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Rule(SOME_DUMP_UUID, "other repository", SOME_KEY).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Rule(SOME_DUMP_UUID, SOME_REPOSITORY, "other key").hashCode());
  }

  @Test
  public void hashcode_ignores_dump_id() {
    assertThat(underTest).hasSameHashCodeAs(new Rule("uuid-8888", SOME_REPOSITORY, SOME_KEY));
  }

  @Test
  public void toString_displays_all_fields() {
    assertThat(underTest).hasToString("Rule{ref='uuid-12334', repository='some repository', key='some key'}");

  }
}

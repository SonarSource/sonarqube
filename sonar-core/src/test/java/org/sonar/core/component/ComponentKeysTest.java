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
package org.sonar.core.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentKeysTest {

  @Test
  public void create_key_from_module_key_path_and_branch() {
    assertThat(ComponentKeys.createKey("module_key", "file", "origin/master")).isEqualTo("module_key:origin/master:file");
    assertThat(ComponentKeys.createKey("module_key", "file", null)).isEqualTo("module_key:file");
    assertThat(ComponentKeys.createKey("module_key", null, null)).isEqualTo("module_key");
  }

  @Test
  public void valid_project_key() {
    assertThat(ComponentKeys.isValidProjectKey("abc")).isTrue();
    assertThat(ComponentKeys.isValidProjectKey("ab_12")).isTrue();
  }

  @Test
  public void invalid_project_key() {
    assertThat(ComponentKeys.isValidProjectKey("0123")).isFalse();

    assertThat(ComponentKeys.isValidProjectKey("ab/12")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("코드품질")).isFalse();

    assertThat(ComponentKeys.isValidProjectKey("")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey(" ")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey(" ab")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("ab ")).isFalse();
  }

  @Test
  public void checkProjectKey_with_correct_keys() {
    ComponentKeys.checkProjectKey("abc");
    ComponentKeys.checkProjectKey("a-b_1.:2");
  }

  @Test
  public void checkProjectKey_fail_if_key_is_empty() {
    assertThatThrownBy(() -> ComponentKeys.checkProjectKey(""))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void checkProjectKey_fail_if_space() {
    assertThatThrownBy(() -> ComponentKeys.checkProjectKey("ab 12"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void checkProjectKey_fail_if_only_digit() {
    assertThatThrownBy(() -> ComponentKeys.checkProjectKey("0123"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Malformed key for '0123'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");
  }

  @Test
  public void checkProjectKey_fail_if_special_characters_not_allowed() {
    assertThatThrownBy(() -> ComponentKeys.checkProjectKey("0123"))
      .isInstanceOf(IllegalArgumentException.class);
  }
}

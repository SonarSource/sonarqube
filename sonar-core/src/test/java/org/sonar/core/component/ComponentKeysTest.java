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
package org.sonar.core.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentKeysTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_effective_key() {
    DefaultInputFile file = mock(DefaultInputFile.class);
    when(file.getProjectRelativePath()).thenReturn("foo/Bar.php");
    assertThat(ComponentKeys.createEffectiveKey("my_project", file)).isEqualTo("my_project:foo/Bar.php");
  }

  @Test
  public void create_key_from_module_key_path_and_branch() {
    assertThat(ComponentKeys.createKey("module_key", "file", "origin/master")).isEqualTo("module_key:origin/master:file");
    assertThat(ComponentKeys.createKey("module_key", "file", null)).isEqualTo("module_key:file");
    assertThat(ComponentKeys.createKey("module_key", null, null)).isEqualTo("module_key");
  }

  @Test
  public void isValidModuleKey() {
    assertThat(ComponentKeys.isValidProjectKey("")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("abc")).isTrue();
    assertThat(ComponentKeys.isValidProjectKey("0123")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidProjectKey("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidProjectKey("ab/12")).isFalse();
  }

  @Test
  public void isValidModuleKeyIssuesMode() {
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("")).isFalse();
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("abc")).isTrue();
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("0123")).isFalse();
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidProjectKeyIssuesMode("ab/12")).isTrue();
  }

  @Test
  public void isValidBranchKey() {
    assertThat(ComponentKeys.isValidLegacyBranch("")).isTrue();
    assertThat(ComponentKeys.isValidLegacyBranch("abc")).isTrue();
    assertThat(ComponentKeys.isValidLegacyBranch("0123")).isTrue();
    assertThat(ComponentKeys.isValidLegacyBranch("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidLegacyBranch("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidLegacyBranch("ab/12")).isTrue();
    assertThat(ComponentKeys.isValidLegacyBranch("ab\\12")).isFalse();
    assertThat(ComponentKeys.isValidLegacyBranch("ab\n")).isFalse();
  }

  @Test
  public void checkModuleKey_with_correct_keys() {
    ComponentKeys.checkProjectKey("abc");
    ComponentKeys.checkProjectKey("a-b_1.:2");
  }

  @Test
  public void checkModuleKey_fail_if_only_digit() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Malformed key for '0123'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    ComponentKeys.checkProjectKey("0123");
  }

  @Test
  public void checkModuleKey_fail_if_key_is_empty() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkProjectKey("");
  }

  @Test
  public void checkModuleKey_fail_if_space() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkProjectKey("ab 12");
  }

  @Test
  public void checkModuleKey_fail_if_special_characters_not_allowed() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkProjectKey("ab/12");
  }
}

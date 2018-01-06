/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;

public class ComponentKeysTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_effective_key() {
    Project project = new Project(ProjectDefinition.create().setKey("my_project"));
    assertThat(ComponentKeys.createEffectiveKey("my_project", project)).isEqualTo("my_project");

    Directory dir = Directory.create("src/org/foo");
    assertThat(ComponentKeys.createEffectiveKey("my_project", dir)).isEqualTo("my_project:src/org/foo");

    InputFile file = mock(InputFile.class);
    when(file.relativePath()).thenReturn("foo/Bar.php");
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
    assertThat(ComponentKeys.isValidModuleKey("")).isFalse();
    assertThat(ComponentKeys.isValidModuleKey("abc")).isTrue();
    assertThat(ComponentKeys.isValidModuleKey("0123")).isFalse();
    assertThat(ComponentKeys.isValidModuleKey("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidModuleKey("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidModuleKey("ab/12")).isFalse();
  }

  @Test
  public void isValidModuleKeyIssuesMode() {
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("")).isFalse();
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("abc")).isTrue();
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("0123")).isFalse();
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidModuleKeyIssuesMode("ab/12")).isTrue();
  }

  @Test
  public void isValidBranchKey() {
    assertThat(ComponentKeys.isValidBranch("")).isTrue();
    assertThat(ComponentKeys.isValidBranch("abc")).isTrue();
    assertThat(ComponentKeys.isValidBranch("0123")).isTrue();
    assertThat(ComponentKeys.isValidBranch("ab 12")).isFalse();
    assertThat(ComponentKeys.isValidBranch("ab_12")).isTrue();
    assertThat(ComponentKeys.isValidBranch("ab/12")).isTrue();
    assertThat(ComponentKeys.isValidBranch("ab\\12")).isFalse();
    assertThat(ComponentKeys.isValidBranch("ab\n")).isFalse();
  }

  @Test
  public void checkModuleKey_with_correct_keys() {
    ComponentKeys.checkModuleKey("abc");
    ComponentKeys.checkModuleKey("a-b_1.:2");
  }

  @Test
  public void checkModuleKey_fail_if_only_digit() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Malformed key for '0123'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    ComponentKeys.checkModuleKey("0123");
  }

  @Test
  public void checkModuleKey_fail_if_key_is_empty() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkModuleKey("");
  }

  @Test
  public void checkModuleKey_fail_if_space() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkModuleKey("ab 12");
  }

  @Test
  public void checkModuleKey_fail_if_special_characters_not_allowed() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentKeys.checkModuleKey("ab/12");
  }
}

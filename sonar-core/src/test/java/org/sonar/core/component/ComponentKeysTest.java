/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.core.component;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentKeysTest {

  @Test
  public void create_effective_key() {
    Project project = new Project("my_project");
    assertThat(ComponentKeys.createEffectiveKey(project, project)).isEqualTo("my_project");

    Directory dir = Directory.create("src/org/foo");
    assertThat(ComponentKeys.createEffectiveKey(project, dir)).isEqualTo("my_project:src/org/foo");

    InputFile file = new DefaultInputFile("foo", "foo/Bar.php");
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

}

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
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirectoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createFromIoFileShouldComputeCorrectKey() throws IOException {
    java.io.File baseDir = temp.newFolder();
    Project project = mock(Project.class);
    when(project.getBaseDir()).thenReturn(baseDir);
    Resource dir = Directory.fromIOFile(new java.io.File(baseDir, "src/foo/bar/"), project);
    assertThat(dir.getKey()).isEqualTo("src/foo/bar");
  }

  @Test
  public void shouldNotStartBySlashAndNotEndBySlash() {
    Resource dir = Directory.create("src/foo/bar/");
    assertThat(dir.getKey()).isEqualTo("src/foo/bar");
    assertThat(dir.getName()).isEqualTo("src/foo/bar");
  }

  @Test
  public void backSlashesShouldBeReplacedBySlashes() {
    Resource dir = Directory.create("  foo\\bar\\     ");
    assertThat(dir.getKey()).isEqualTo("foo/bar");
  }

  @Test
  public void directoryHasNoParents() {
    Resource dir = Directory.create("foo/bar");
    assertThat(dir.getParent()).isNull();
  }

  @Test
  public void shouldHaveOnlyOneLevelOfDirectory() {
    assertThat(Directory.create("one/two/third").getParent()).isNull();
    assertThat(Directory.create("one").getParent()).isNull();
  }

  @Test
  public void parseDirectoryKey() {
    assertThat(Directory.parseKey("/foo/bar")).isEqualTo("foo/bar");
  }

  @Test
  public void matchExclusionPatterns() {
    Directory directory = Directory.create("src/one/two/third");
    assertThat(directory.matchFilePattern("one/two/*.java")).isFalse();
    assertThat(directory.matchFilePattern("false")).isFalse();
    assertThat(directory.matchFilePattern("two/one/**")).isFalse();
    assertThat(directory.matchFilePattern("other*/**")).isFalse();

    assertThat(directory.matchFilePattern("src/one*/**")).isTrue();
    assertThat(directory.matchFilePattern("src/one/t?o/**")).isTrue();
    assertThat(directory.matchFilePattern("**/*")).isTrue();
    assertThat(directory.matchFilePattern("**")).isTrue();
    assertThat(directory.matchFilePattern("src/one/two/*")).isTrue();
    assertThat(directory.matchFilePattern("/src/one/two/*")).isTrue();
    assertThat(directory.matchFilePattern("src/one/**")).isTrue();
  }
}

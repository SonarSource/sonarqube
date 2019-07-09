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
package org.sonar.api.batch.fs.internal.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.PathPattern;

import static org.assertj.core.api.Assertions.assertThat;

public class PathPatternTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private Path baseDir;

  @Before
  public void setUp() throws IOException {
    baseDir = temp.newFolder().toPath();
  }

  @Test
  public void match_relative_path() {
    PathPattern pattern = PathPattern.create("**/*Foo.java");
    assertThat(pattern.toString()).isEqualTo("**/*Foo.java");

    IndexedFile indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.java", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isTrue();

    // case sensitive by default
    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.JAVA", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isFalse();

    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/Other.java", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isFalse();
  }

  @Test
  public void match_relative_path_and_insensitive_file_extension() throws Exception {
    PathPattern pattern = PathPattern.create("**/*Foo.java");

    IndexedFile indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.JAVA", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()), false)).isTrue();

    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/Other.java", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()), false)).isFalse();
  }

  @Test
  public void match_absolute_path() throws Exception {
    PathPattern pattern = PathPattern.create("file:**/src/main/**Foo.java");
    assertThat(pattern.toString()).isEqualTo("file:**/src/main/**Foo.java");

    IndexedFile indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.java", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isTrue();

    // case sensitive by default
    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.JAVA", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isFalse();

    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/Other.java", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()))).isFalse();
  }

  @Test
  public void match_absolute_path_and_insensitive_file_extension() throws Exception {
    PathPattern pattern = PathPattern.create("file:**/src/main/**Foo.java");
    assertThat(pattern.toString()).isEqualTo("file:**/src/main/**Foo.java");

    IndexedFile indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/MyFoo.JAVA", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()), false)).isTrue();

    indexedFile = new DefaultIndexedFile("ABCDE", baseDir, "src/main/java/org/Other.JAVA", null);
    assertThat(pattern.match(indexedFile.path(), Paths.get(indexedFile.relativePath()), false)).isFalse();
  }

  @Test
  public void create_array_of_patterns() {
    PathPattern[] patterns = PathPattern.create(new String[] {
      "**/src/main/**Foo.java",
      "file:**/src/main/**Bar.java"
    });
    assertThat(patterns).hasSize(2);
    assertThat(patterns[0].toString()).isEqualTo("**/src/main/**Foo.java");
    assertThat(patterns[1].toString()).isEqualTo("file:**/src/main/**Bar.java");
  }
}

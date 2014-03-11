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
package org.sonar.api.batch.fs;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class FilePredicatesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultInputFile javaFile;

  @Before
  public void before() throws IOException {
    javaFile = new DefaultInputFile("src/main/java/struts/Action.java")
      .setFile(temp.newFile("Action.java"))
      .setLanguage("java")
      .setStatus(InputFile.Status.ADDED);
  }

  @Test
  public void all() throws Exception {
    assertThat(FilePredicates.all().apply(javaFile)).isTrue();
  }

  @Test
  public void none() throws Exception {
    assertThat(FilePredicates.none().apply(javaFile)).isFalse();
  }

  @Test
  public void matches_inclusion_pattern() throws Exception {
    assertThat(FilePredicates.matchesPathPattern("src/main/**/Action.java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.matchesPathPattern("Action.java").apply(javaFile)).isFalse();
    assertThat(FilePredicates.matchesPathPattern("src/**/*.php").apply(javaFile)).isFalse();
  }

  @Test
  public void matches_inclusion_patterns() throws Exception {
    assertThat(FilePredicates.matchesPathPatterns(new String[]{"src/other/**.java", "src/main/**/Action.java"}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.matchesPathPatterns(new String[]{}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.matchesPathPatterns(new String[]{"src/other/**.java", "src/**/*.php"}).apply(javaFile)).isFalse();
  }

  @Test
  public void does_not_match_exclusion_pattern() throws Exception {
    assertThat(FilePredicates.doesNotMatchPathPattern("src/main/**/Action.java").apply(javaFile)).isFalse();
    assertThat(FilePredicates.doesNotMatchPathPattern("Action.java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.doesNotMatchPathPattern("src/**/*.php").apply(javaFile)).isTrue();
  }

  @Test
  public void does_not_match_exclusion_patterns() throws Exception {
    assertThat(FilePredicates.doesNotMatchPathPatterns(new String[]{}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.doesNotMatchPathPatterns(new String[]{"src/other/**.java", "src/**/*.php"}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.doesNotMatchPathPatterns(new String[]{"src/other/**.java", "src/main/**/Action.java"}).apply(javaFile)).isFalse();
  }

  @Test
  public void has_relative_path() throws Exception {
    assertThat(FilePredicates.hasRelativePath("src/main/java/struts/Action.java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasRelativePath("src/main/java/struts/Other.java").apply(javaFile)).isFalse();

    // path is normalized
    assertThat(FilePredicates.hasRelativePath("src/main/java/../java/struts/Action.java").apply(javaFile)).isTrue();

    assertThat(FilePredicates.hasRelativePath("src\\main\\java\\struts\\Action.java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasRelativePath("src\\main\\java\\struts\\Other.java").apply(javaFile)).isFalse();
    assertThat(FilePredicates.hasRelativePath("src\\main\\java\\struts\\..\\struts\\Action.java").apply(javaFile)).isTrue();
  }

  @Test
  public void has_absolute_path() throws Exception {
    String path = javaFile.file().getAbsolutePath();
    assertThat(FilePredicates.hasAbsolutePath(path).apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasAbsolutePath(FilenameUtils.separatorsToWindows(path)).apply(javaFile)).isTrue();

    assertThat(FilePredicates.hasAbsolutePath(temp.newFile().getAbsolutePath()).apply(javaFile)).isFalse();
    assertThat(FilePredicates.hasAbsolutePath("src/main/java/struts/Action.java").apply(javaFile)).isFalse();
  }

  @Test
  public void has_path() throws Exception {
    // is relative path
    assertThat(FilePredicates.hasPath("src/main/java/struts/Action.java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasPath("src/main/java/struts/Other.java").apply(javaFile)).isFalse();

    // is absolute path
    String path = javaFile.file().getAbsolutePath();
    assertThat(FilePredicates.hasAbsolutePath(path).apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasPath(temp.newFile().getAbsolutePath()).apply(javaFile)).isFalse();
  }

  @Test
  public void is_file() throws Exception {
    // relative file
    assertThat(FilePredicates.is(new File(javaFile.relativePath())).apply(javaFile)).isTrue();


    // absolute file
    assertThat(FilePredicates.is(javaFile.file()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.is(javaFile.file().getAbsoluteFile()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.is(javaFile.file().getCanonicalFile()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.is(new File(javaFile.file().toURI())).apply(javaFile)).isTrue();
    assertThat(FilePredicates.is(temp.newFile()).apply(javaFile)).isFalse();
  }

  @Test
  public void has_language() throws Exception {
    assertThat(FilePredicates.hasLanguage("java").apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasLanguage("php").apply(javaFile)).isFalse();
  }

  @Test
  public void has_languages() throws Exception {
    assertThat(FilePredicates.hasLanguages(Arrays.asList("java", "php")).apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasLanguages(Arrays.asList("cobol", "php")).apply(javaFile)).isFalse();
    assertThat(FilePredicates.hasLanguages(Collections.<String>emptyList()).apply(javaFile)).isTrue();
  }

  @Test
  public void has_status() throws Exception {
    assertThat(FilePredicates.hasStatus(InputFile.Status.ADDED).apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasStatus(InputFile.Status.CHANGED).apply(javaFile)).isFalse();
  }

  @Test
  public void has_type() throws Exception {
    assertThat(FilePredicates.hasType(InputFile.Type.MAIN).apply(javaFile)).isTrue();
    assertThat(FilePredicates.hasType(InputFile.Type.TEST).apply(javaFile)).isFalse();
  }

  @Test
  public void not() throws Exception {
    assertThat(FilePredicates.not(FilePredicates.hasType(InputFile.Type.MAIN)).apply(javaFile)).isFalse();
    assertThat(FilePredicates.not(FilePredicates.hasType(InputFile.Type.TEST)).apply(javaFile)).isTrue();
  }

  @Test
  public void and() throws Exception {
    // empty
    assertThat(FilePredicates.and().apply(javaFile)).isTrue();
    assertThat(FilePredicates.and(new FilePredicate[0]).apply(javaFile)).isTrue();
    assertThat(FilePredicates.and(Collections.<FilePredicate>emptyList()).apply(javaFile)).isTrue();

    // two arguments
    assertThat(FilePredicates.and(FilePredicates.all(), FilePredicates.all()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.and(FilePredicates.all(), FilePredicates.none()).apply(javaFile)).isFalse();
    assertThat(FilePredicates.and(FilePredicates.none(), FilePredicates.all()).apply(javaFile)).isFalse();

    // collection
    assertThat(FilePredicates.and(Arrays.asList(FilePredicates.all(), FilePredicates.all())).apply(javaFile)).isTrue();
    assertThat(FilePredicates.and(Arrays.asList(FilePredicates.all(), FilePredicates.none())).apply(javaFile)).isFalse();

    // array
    assertThat(FilePredicates.and(new FilePredicate[]{FilePredicates.all(), FilePredicates.all()}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.and(new FilePredicate[]{FilePredicates.all(), FilePredicates.none()}).apply(javaFile)).isFalse();
  }

  @Test
  public void or() throws Exception {
    // empty
    assertThat(FilePredicates.or().apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(new FilePredicate[0]).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(Collections.<FilePredicate>emptyList()).apply(javaFile)).isTrue();

    // two arguments
    assertThat(FilePredicates.or(FilePredicates.all(), FilePredicates.all()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(FilePredicates.all(), FilePredicates.none()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(FilePredicates.none(), FilePredicates.all()).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(FilePredicates.none(), FilePredicates.none()).apply(javaFile)).isFalse();

    // collection
    assertThat(FilePredicates.or(Arrays.asList(FilePredicates.all(), FilePredicates.all())).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(Arrays.asList(FilePredicates.all(), FilePredicates.none())).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(Arrays.asList(FilePredicates.none(), FilePredicates.none())).apply(javaFile)).isFalse();

    // array
    assertThat(FilePredicates.or(new FilePredicate[]{FilePredicates.all(), FilePredicates.all()}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(new FilePredicate[]{FilePredicates.all(), FilePredicates.none()}).apply(javaFile)).isTrue();
    assertThat(FilePredicates.or(new FilePredicate[]{FilePredicates.none(), FilePredicates.none()}).apply(javaFile)).isFalse();
  }
}

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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultFilePredicatesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Path moduleBasePath;

  @Before
  public void setUp() throws IOException {
    moduleBasePath = temp.newFolder().toPath();
  }

  InputFile javaFile;
  FilePredicates predicates;

  @Before
  public void before() throws IOException {
    predicates = new DefaultFilePredicates(temp.newFolder().toPath());
    javaFile = new TestInputFileBuilder("foo", "src/main/java/struts/Action.java")
      .setModuleBaseDir(moduleBasePath)
      .setLanguage("java")
      .setStatus(Status.SAME)
      .build();

  }

  @Test
  public void all() {
    assertThat(predicates.all().apply(javaFile)).isTrue();
  }

  @Test
  public void none() {
    assertThat(predicates.none().apply(javaFile)).isFalse();
  }

  @Test
  public void matches_inclusion_pattern() {
    assertThat(predicates.matchesPathPattern("src/main/**/Action.java").apply(javaFile)).isTrue();
    assertThat(predicates.matchesPathPattern("Action.java").apply(javaFile)).isFalse();
    assertThat(predicates.matchesPathPattern("src/**/*.php").apply(javaFile)).isFalse();
  }

  @Test
  public void matches_inclusion_patterns() {
    assertThat(predicates.matchesPathPatterns(new String[] {"src/other/**.java", "src/main/**/Action.java"}).apply(javaFile)).isTrue();
    assertThat(predicates.matchesPathPatterns(new String[] {}).apply(javaFile)).isTrue();
    assertThat(predicates.matchesPathPatterns(new String[] {"src/other/**.java", "src/**/*.php"}).apply(javaFile)).isFalse();
  }

  @Test
  public void does_not_match_exclusion_pattern() {
    assertThat(predicates.doesNotMatchPathPattern("src/main/**/Action.java").apply(javaFile)).isFalse();
    assertThat(predicates.doesNotMatchPathPattern("Action.java").apply(javaFile)).isTrue();
    assertThat(predicates.doesNotMatchPathPattern("src/**/*.php").apply(javaFile)).isTrue();
  }

  @Test
  public void does_not_match_exclusion_patterns() {
    assertThat(predicates.doesNotMatchPathPatterns(new String[] {}).apply(javaFile)).isTrue();
    assertThat(predicates.doesNotMatchPathPatterns(new String[] {"src/other/**.java", "src/**/*.php"}).apply(javaFile)).isTrue();
    assertThat(predicates.doesNotMatchPathPatterns(new String[] {"src/other/**.java", "src/main/**/Action.java"}).apply(javaFile)).isFalse();
  }

  @Test
  public void has_relative_path() {
    assertThat(predicates.hasRelativePath("src/main/java/struts/Action.java").apply(javaFile)).isTrue();
    assertThat(predicates.hasRelativePath("src/main/java/struts/Other.java").apply(javaFile)).isFalse();

    // path is normalized
    assertThat(predicates.hasRelativePath("src/main/java/../java/struts/Action.java").apply(javaFile)).isTrue();

    assertThat(predicates.hasRelativePath("src\\main\\java\\struts\\Action.java").apply(javaFile)).isTrue();
    assertThat(predicates.hasRelativePath("src\\main\\java\\struts\\Other.java").apply(javaFile)).isFalse();
    assertThat(predicates.hasRelativePath("src\\main\\java\\struts\\..\\struts\\Action.java").apply(javaFile)).isTrue();
  }

  @Test
  public void has_absolute_path() throws Exception {
    String path = javaFile.file().getAbsolutePath();
    assertThat(predicates.hasAbsolutePath(path).apply(javaFile)).isTrue();
    assertThat(predicates.hasAbsolutePath(path.replaceAll("/", "\\\\")).apply(javaFile)).isTrue();

    assertThat(predicates.hasAbsolutePath(temp.newFile().getAbsolutePath()).apply(javaFile)).isFalse();
    assertThat(predicates.hasAbsolutePath("src/main/java/struts/Action.java").apply(javaFile)).isFalse();
  }

  @Test
  public void has_uri() throws Exception {
    URI uri = javaFile.uri();
    assertThat(predicates.hasURI(uri).apply(javaFile)).isTrue();

    assertThat(predicates.hasURI(temp.newFile().toURI()).apply(javaFile)).isFalse();
  }

  @Test
  public void has_path() throws Exception {
    // is relative path
    assertThat(predicates.hasPath("src/main/java/struts/Action.java").apply(javaFile)).isTrue();
    assertThat(predicates.hasPath("src/main/java/struts/Other.java").apply(javaFile)).isFalse();

    // is absolute path
    String path = javaFile.file().getAbsolutePath();
    assertThat(predicates.hasAbsolutePath(path).apply(javaFile)).isTrue();
    assertThat(predicates.hasPath(temp.newFile().getAbsolutePath()).apply(javaFile)).isFalse();
  }

  @Test
  public void is_file() throws Exception {
    // relative file
    assertThat(predicates.is(new File(javaFile.relativePath())).apply(javaFile)).isTrue();

    // absolute file
    assertThat(predicates.is(javaFile.file()).apply(javaFile)).isTrue();
    assertThat(predicates.is(javaFile.file().getAbsoluteFile()).apply(javaFile)).isTrue();
    assertThat(predicates.is(new File(javaFile.file().toURI())).apply(javaFile)).isTrue();
    assertThat(predicates.is(temp.newFile()).apply(javaFile)).isFalse();
  }

  @Test
  public void has_language() {
    assertThat(predicates.hasLanguage("java").apply(javaFile)).isTrue();
    assertThat(predicates.hasLanguage("php").apply(javaFile)).isFalse();
  }

  @Test
  public void has_languages() {
    assertThat(predicates.hasLanguages(Arrays.asList("java", "php")).apply(javaFile)).isTrue();
    assertThat(predicates.hasLanguages(Arrays.asList("cobol", "php")).apply(javaFile)).isFalse();
    assertThat(predicates.hasLanguages(Collections.<String>emptyList()).apply(javaFile)).isTrue();
  }

  @Test
  public void has_type() {
    assertThat(predicates.hasType(InputFile.Type.MAIN).apply(javaFile)).isTrue();
    assertThat(predicates.hasType(InputFile.Type.TEST).apply(javaFile)).isFalse();
  }

  @Test
  public void has_status() {
    assertThat(predicates.hasAnyStatus().apply(javaFile)).isTrue();
    assertThat(predicates.hasStatus(InputFile.Status.SAME).apply(javaFile)).isTrue();
    assertThat(predicates.hasStatus(InputFile.Status.ADDED).apply(javaFile)).isFalse();
  }

  @Test
  public void not() {
    assertThat(predicates.not(predicates.hasType(InputFile.Type.MAIN)).apply(javaFile)).isFalse();
    assertThat(predicates.not(predicates.hasType(InputFile.Type.TEST)).apply(javaFile)).isTrue();
  }

  @Test
  public void and() {
    // empty
    assertThat(predicates.and().apply(javaFile)).isTrue();
    assertThat(predicates.and(new FilePredicate[0]).apply(javaFile)).isTrue();
    assertThat(predicates.and(Collections.<FilePredicate>emptyList()).apply(javaFile)).isTrue();

    // two arguments
    assertThat(predicates.and(predicates.all(), predicates.all()).apply(javaFile)).isTrue();
    assertThat(predicates.and(predicates.all(), predicates.none()).apply(javaFile)).isFalse();
    assertThat(predicates.and(predicates.none(), predicates.all()).apply(javaFile)).isFalse();

    // collection
    assertThat(predicates.and(Arrays.asList(predicates.all(), predicates.all())).apply(javaFile)).isTrue();
    assertThat(predicates.and(Arrays.asList(predicates.all(), predicates.none())).apply(javaFile)).isFalse();

    // array
    assertThat(predicates.and(new FilePredicate[] {predicates.all(), predicates.all()}).apply(javaFile)).isTrue();
    assertThat(predicates.and(new FilePredicate[] {predicates.all(), predicates.none()}).apply(javaFile)).isFalse();
  }

  @Test
  public void or() {
    // empty
    assertThat(predicates.or().apply(javaFile)).isTrue();
    assertThat(predicates.or(new FilePredicate[0]).apply(javaFile)).isTrue();
    assertThat(predicates.or(Collections.<FilePredicate>emptyList()).apply(javaFile)).isTrue();

    // two arguments
    assertThat(predicates.or(predicates.all(), predicates.all()).apply(javaFile)).isTrue();
    assertThat(predicates.or(predicates.all(), predicates.none()).apply(javaFile)).isTrue();
    assertThat(predicates.or(predicates.none(), predicates.all()).apply(javaFile)).isTrue();
    assertThat(predicates.or(predicates.none(), predicates.none()).apply(javaFile)).isFalse();

    // collection
    assertThat(predicates.or(Arrays.asList(predicates.all(), predicates.all())).apply(javaFile)).isTrue();
    assertThat(predicates.or(Arrays.asList(predicates.all(), predicates.none())).apply(javaFile)).isTrue();
    assertThat(predicates.or(Arrays.asList(predicates.none(), predicates.none())).apply(javaFile)).isFalse();

    // array
    assertThat(predicates.or(new FilePredicate[] {predicates.all(), predicates.all()}).apply(javaFile)).isTrue();
    assertThat(predicates.or(new FilePredicate[] {predicates.all(), predicates.none()}).apply(javaFile)).isTrue();
    assertThat(predicates.or(new FilePredicate[] {predicates.none(), predicates.none()}).apply(javaFile)).isFalse();
  }

  @Test
  public void hasFilename() {
    assertThat(predicates.hasFilename("Action.java").apply(javaFile)).isTrue();
  }

  @Test
  public void hasExtension() {
    assertThat(predicates.hasExtension("java").apply(javaFile)).isTrue();
  }
}

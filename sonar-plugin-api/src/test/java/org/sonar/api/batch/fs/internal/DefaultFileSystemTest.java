/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.batch.fs.internal;

import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FilePredicates;

import java.io.File;
import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_directories() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();

    File basedir = temp.newFolder();
    fs.setBaseDir(basedir);
    assertThat(fs.baseDir()).isAbsolute().isDirectory().exists();
    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());

    File workdir = temp.newFolder();
    fs.setWorkDir(workdir);
    assertThat(fs.workDir()).isAbsolute().isDirectory().exists();
    assertThat(fs.workDir().getCanonicalPath()).isEqualTo(workdir.getCanonicalPath());
  }

  @Test
  public void test_encoding() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();

    assertThat(fs.isDefaultJvmEncoding()).isTrue();
    assertThat(fs.encoding()).isEqualTo(Charset.defaultCharset());

    fs.setEncoding(Charsets.ISO_8859_1);
    assertThat(fs.encoding()).isEqualTo(Charsets.ISO_8859_1);
    assertThat(fs.isDefaultJvmEncoding()).isFalse();
  }

  @Test
  public void add_languages() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();

    assertThat(fs.languages()).isEmpty();

    fs.addLanguages("java", "php", "cobol");
    assertThat(fs.languages()).containsOnly("cobol", "java", "php");
  }

  @Test
  public void files() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();

    assertThat(fs.inputFiles(FilePredicates.all())).isEmpty();

    fs.add(new DefaultInputFile("src/Foo.php").setLanguage("php").setFile(temp.newFile()));
    fs.add(new DefaultInputFile("src/Bar.java").setLanguage("java").setFile(temp.newFile()));
    fs.add(new DefaultInputFile("src/Baz.java").setLanguage("java").setFile(temp.newFile()));

    // no language
    fs.add(new DefaultInputFile("src/readme.txt").setFile(temp.newFile()));

    assertThat(fs.inputFile(FilePredicates.hasRelativePath("src/Bar.java"))).isNotNull();
    assertThat(fs.inputFile(FilePredicates.hasRelativePath("does/not/exist"))).isNull();

    assertThat(fs.files(FilePredicates.all())).hasSize(4);
    assertThat(fs.files(FilePredicates.hasLanguage("java"))).hasSize(2);
    assertThat(fs.files(FilePredicates.hasLanguage("cobol"))).isEmpty();

    assertThat(fs.hasFiles(FilePredicates.all())).isTrue();
    assertThat(fs.hasFiles(FilePredicates.hasLanguage("java"))).isTrue();
    assertThat(fs.hasFiles(FilePredicates.hasLanguage("cobol"))).isFalse();

    assertThat(fs.inputFiles(FilePredicates.all())).hasSize(4);
    assertThat(fs.inputFiles(FilePredicates.hasLanguage("php"))).hasSize(1);
    assertThat(fs.inputFiles(FilePredicates.hasLanguage("java"))).hasSize(2);
    assertThat(fs.inputFiles(FilePredicates.hasLanguage("cobol"))).isEmpty();

    assertThat(fs.languages()).containsOnly("java", "php");
  }

  @Test
  public void input_file_returns_null_if_file_not_found() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();
    assertThat(fs.inputFile(FilePredicates.hasRelativePath("src/Bar.java"))).isNull();
  }

  @Test
  public void input_file_fails_if_too_many_results() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("expected one element");

    DefaultFileSystem fs = new DefaultFileSystem();
    fs.add(new DefaultInputFile("src/Bar.java").setLanguage("java").setFile(temp.newFile()));
    fs.add(new DefaultInputFile("src/Baz.java").setLanguage("java").setFile(temp.newFile()));

    fs.inputFile(FilePredicates.all());
  }

  @Test
  public void input_file_supports_non_indexed_predicates() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem();
    fs.add(new DefaultInputFile("src/Bar.java").setLanguage("java").setFile(temp.newFile()));

    // it would fail if more than one java file
    assertThat(fs.inputFile(FilePredicates.hasLanguage("java"))).isNotNull();
  }
}

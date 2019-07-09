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

import java.io.File;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultFileSystem fs;

  private File basedir;

  @Before
  public void prepare() throws Exception {
    basedir = temp.newFolder();
    fs = new DefaultFileSystem(basedir.toPath());
  }

  @Test
  public void test_directories() throws Exception {
    assertThat(fs.baseDir()).isAbsolute().isDirectory().exists();
    assertThat(fs.baseDir().getCanonicalPath()).isEqualTo(basedir.getCanonicalPath());

    File workdir = temp.newFolder();
    fs.setWorkDir(workdir.toPath());
    assertThat(fs.workDir()).isAbsolute().isDirectory().exists();
    assertThat(fs.workDir().getCanonicalPath()).isEqualTo(workdir.getCanonicalPath());
  }

  @Test
  public void test_encoding() throws Exception {
    fs.setEncoding(Charset.forName("ISO-8859-1"));
    assertThat(fs.encoding()).isEqualTo(Charset.forName("ISO-8859-1"));
  }

  @Test
  public void add_languages() {
    assertThat(fs.languages()).isEmpty();

    fs.add(new TestInputFileBuilder("foo", "src/Foo.php").setLanguage("php").build());
    fs.add(new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());

    assertThat(fs.languages()).containsOnly("java", "php");
  }

  @Test
  public void files() {
    assertThat(fs.inputFiles(fs.predicates().all())).isEmpty();

    fs.add(new TestInputFileBuilder("foo", "src/Foo.php").setLanguage("php").build());
    fs.add(new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());
    fs.add(new TestInputFileBuilder("foo", "src/Baz.java").setLanguage("java").build());

    // no language
    fs.add(new TestInputFileBuilder("foo", "src/readme.txt").build());

    assertThat(fs.inputFile(fs.predicates().hasRelativePath("src/Bar.java"))).isNotNull();
    assertThat(fs.inputFile(fs.predicates().hasRelativePath("does/not/exist"))).isNull();

    assertThat(fs.inputFile(fs.predicates().hasAbsolutePath(new File(basedir, "src/Bar.java").getAbsolutePath()))).isNotNull();
    assertThat(fs.inputFile(fs.predicates().hasAbsolutePath(new File(basedir, "does/not/exist").getAbsolutePath()))).isNull();
    assertThat(fs.inputFile(fs.predicates().hasAbsolutePath(new File(basedir, "../src/Bar.java").getAbsolutePath()))).isNull();

    assertThat(fs.inputFile(fs.predicates().hasURI(new File(basedir, "src/Bar.java").toURI()))).isNotNull();
    assertThat(fs.inputFile(fs.predicates().hasURI(new File(basedir, "does/not/exist").toURI()))).isNull();
    assertThat(fs.inputFile(fs.predicates().hasURI(new File(basedir, "../src/Bar.java").toURI()))).isNull();

    assertThat(fs.files(fs.predicates().all())).hasSize(4);
    assertThat(fs.files(fs.predicates().hasLanguage("java"))).hasSize(2);
    assertThat(fs.files(fs.predicates().hasLanguage("cobol"))).isEmpty();

    assertThat(fs.hasFiles(fs.predicates().all())).isTrue();
    assertThat(fs.hasFiles(fs.predicates().hasLanguage("java"))).isTrue();
    assertThat(fs.hasFiles(fs.predicates().hasLanguage("cobol"))).isFalse();

    assertThat(fs.inputFiles(fs.predicates().all())).hasSize(4);
    assertThat(fs.inputFiles(fs.predicates().hasLanguage("php"))).hasSize(1);
    assertThat(fs.inputFiles(fs.predicates().hasLanguage("java"))).hasSize(2);
    assertThat(fs.inputFiles(fs.predicates().hasLanguage("cobol"))).isEmpty();

    assertThat(fs.languages()).containsOnly("java", "php");
  }

  @Test
  public void input_file_returns_null_if_file_not_found() {
    assertThat(fs.inputFile(fs.predicates().hasRelativePath("src/Bar.java"))).isNull();
    assertThat(fs.inputFile(fs.predicates().hasLanguage("cobol"))).isNull();
  }

  @Test
  public void input_file_fails_if_too_many_results() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("expected one element");

    fs.add(new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());
    fs.add(new TestInputFileBuilder("foo", "src/Baz.java").setLanguage("java").build());

    fs.inputFile(fs.predicates().all());
  }

  @Test
  public void input_file_supports_non_indexed_predicates() {
    fs.add(new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());

    // it would fail if more than one java file
    assertThat(fs.inputFile(fs.predicates().hasLanguage("java"))).isNotNull();
  }
}

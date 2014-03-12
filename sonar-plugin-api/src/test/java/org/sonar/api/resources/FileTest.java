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
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createFromIoFileShouldComputeCorrectKey() throws IOException {
    java.io.File baseDir = temp.newFolder();
    Project project = mock(Project.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.getBasedir()).thenReturn(baseDir);
    Resource file = File.fromIOFile(new java.io.File(baseDir, "src/foo/bar/toto.sql"), project);
    assertThat(file.getKey(), is("src/foo/bar/toto.sql"));
  }

  @Test
  public void trimKeyAndName() {
    File file = new File("   foo/bar/  ", "  toto.sql  ");
    assertThat(file.getDeprecatedKey(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
  }

  @Test
  public void parentIsDirectory() {
    File file = File.create("src/foo/bar/toto.sql", "foo/bar/toto.sql", null, false);
    assertThat(file.getKey(), is("src/foo/bar/toto.sql"));
    assertThat(file.getDeprecatedKey(), is("foo/bar/toto.sql"));
    assertThat(file.getLongName(), is("src/foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("src/foo/bar"));
    assertThat(ResourceUtils.isSpace(file.getParent()), is(true));
  }

  @Test
  public void parentIsDirectoryWithDeprecatedKey() {
    File file = new File("   foo/bar/", "toto.sql  ");
    assertThat(file.getDeprecatedKey(), is("foo/bar/toto.sql"));
    assertThat(file.getParent().getDeprecatedKey(), is("foo/bar"));
    assertThat(ResourceUtils.isSpace(file.getParent()), is(true));
  }

  @Test
  public void rootFilesHaveParent() {
    File file = File.create("toto.sql", "toto.sql", null, false);
    assertThat(file.getKey(), is("toto.sql"));
    assertThat(file.getDeprecatedKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("/"));
    assertThat(file.getParent().getDeprecatedKey(), is(Directory.ROOT));
  }

  @Test
  public void newFileByDeprecatedKey() {
    File file = new File("toto.sql");
    assertThat(file.getDeprecatedKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getDeprecatedKey(), is(Directory.ROOT));
    assertThat(file.getScope(), is(Resource.SCOPE_ENTITY));
    assertThat(file.getQualifier(), is(Resource.QUALIFIER_FILE));

    file = new File("foo/bar/toto.sql");
    assertThat(file.getDeprecatedKey(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getDeprecatedKey(), is("foo/bar"));

    file = new File("/foo/bar/toto.sql ");
    assertThat(file.getDeprecatedKey(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getDeprecatedKey(), is("foo/bar"));
  }

  @Test
  public void setLanguage() {
    Language lang = mock(Language.class);
    File file = new File(lang, "Foo.java");
    assertThat(file.getLanguage(), is(lang));

    file = new File(lang, "org/sonar", "Foo.java");
    assertThat(file.getLanguage(), is(lang));
    assertThat(file.getParent().getLanguage(), nullValue());
  }

  @Test
  public void matchAntPatterns() {
    File file = File.create("src/one/two/foo.sql", "one/two/foo.sql", null, false);
    assertThat(file.matchFilePattern("/src/one/two/*.java")).isFalse();
    assertThat(file.matchFilePattern("false")).isFalse();
    assertThat(file.matchFilePattern("two/one/**")).isFalse();
    assertThat(file.matchFilePattern("other*/**")).isFalse();

    assertThat(file.matchFilePattern("/src/one*/**/*.sql")).isTrue();
    assertThat(file.matchFilePattern("/src/one/t?o/**/*")).isTrue();
    assertThat(file.matchFilePattern("**/*")).isTrue();
    assertThat(file.matchFilePattern("src/one/two/*")).isTrue();
    assertThat(file.matchFilePattern("/src/one/two/*")).isTrue();
    assertThat(file.matchFilePattern("src/**")).isTrue();
  }
}

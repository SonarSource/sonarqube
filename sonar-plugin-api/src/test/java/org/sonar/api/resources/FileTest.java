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
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createFromIoFileShouldComputeCorrectKey() throws IOException {
    java.io.File baseDir = temp.newFolder();
    Project project = mock(Project.class);
    when(project.getBaseDir()).thenReturn(baseDir);
    Resource file = File.fromIOFile(new java.io.File(baseDir, "src/foo/bar/toto.sql"), project);
    assertThat(file.getKey()).isEqualTo("src/foo/bar/toto.sql");
  }

  @Test
  public void trimKeyAndName() {
    File file = File.create("   foo/bar/toto.sql  ");
    assertThat(file.getKey()).isEqualTo("foo/bar/toto.sql");
    assertThat(file.getName()).isEqualTo("toto.sql");
  }

  @Test
  public void parentIsDirectory() {
    File file = File.create("src/foo/bar/toto.sql", null, false);
    assertThat(file.getKey()).isEqualTo("src/foo/bar/toto.sql");
    assertThat(file.getLongName()).isEqualTo("src/foo/bar/toto.sql");
    assertThat(file.getName()).isEqualTo("toto.sql");
    assertThat(file.getParent().getKey()).isEqualTo("src/foo/bar");
    assertThat(ResourceUtils.isSpace(file.getParent())).isEqualTo(true);
  }

  @Test
  public void rootFilesHaveParent() {
    File file = File.create("toto.sql", null, false);
    assertThat(file.getKey()).isEqualTo("toto.sql");
    assertThat(file.getName()).isEqualTo("toto.sql");
    assertThat(file.getParent().getKey()).isEqualTo("/");
  }

  @Test
  public void setLanguage() {
    Language lang = new AbstractLanguage("java", "Java") {

      @Override
      public String[] getFileSuffixes() {
        return null;
      }
    };

    File file = File.create("Foo.java", lang, false);
    assertThat(file.getLanguage()).isEqualTo(lang);

    file = File.create("org/sonar/Foo.java", lang, false);
    assertThat(file.getLanguage()).isEqualTo(lang);
    assertThat(file.language()).isEqualTo("java");
    assertThat(file.getParent().getLanguage()).isNull();
  }

  @Test
  public void matchAntPatterns() {
    File file = File.create("src/one/two/foo.sql", null, false);
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

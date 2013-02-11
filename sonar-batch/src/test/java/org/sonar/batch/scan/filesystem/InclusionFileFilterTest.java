/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InclusionFileFilterTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_accept() throws IOException {
    InclusionFileFilter filter = InclusionFileFilter.create(FileFilter.FileType.SOURCE, "**/*Dao.java");
    File acceptedFile = temp.newFile("FooDao.java");

    FileFilterContext context = new FileFilterContext(mock(ModuleFileSystem.class), FileFilter.FileType.SOURCE);
    context.setFileRelativePath("com/mycompany/FooDao.java");
    assertThat(filter.accept(acceptedFile, context)).isTrue();

    context = new FileFilterContext(mock(ModuleFileSystem.class), FileFilter.FileType.TEST);
    context.setFileRelativePath("com/mycompany/Foo.java");
    assertThat(filter.accept(acceptedFile, context)).isTrue();
  }

  @Test
  public void should_exclude() throws IOException {
    InclusionFileFilter filter = InclusionFileFilter.create(FileFilter.FileType.SOURCE, "**/*Dao.java");
    File excludedFile = temp.newFile("Foo.java");

    FileFilterContext context = new FileFilterContext(mock(ModuleFileSystem.class), FileFilter.FileType.SOURCE);
    context.setFileRelativePath("com/mycompany/Foo.java");
    assertThat(filter.accept(excludedFile, context)).isFalse();

    context = new FileFilterContext(mock(ModuleFileSystem.class), FileFilter.FileType.TEST);
    context.setFileRelativePath("com/mycompany/Foo.java");
    assertThat(filter.accept(excludedFile, context)).isTrue();
  }

  @Test
  public void should_trim_pattern() throws IOException {
    InclusionFileFilter filter = InclusionFileFilter.create(FileFilter.FileType.SOURCE, "  **/*Dao.java   ");
    assertThat(filter.pattern().toString()).isEqualTo("**/*Dao.java");
  }

  @Test
  public void ignore_if_include_world() throws IOException {
    assertThat(InclusionFileFilter.create(FileFilter.FileType.SOURCE, "  **/*  ")).isNull();
  }
}

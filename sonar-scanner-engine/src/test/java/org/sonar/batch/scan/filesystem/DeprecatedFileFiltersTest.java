/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.FileType;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeprecatedFileFiltersTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  FileSystemFilter filter = mock(FileSystemFilter.class);

  @Test
  public void no_filters() {
    DeprecatedFileFilters filters = new DeprecatedFileFilters();

    InputFile inputFile = new DefaultInputFile("foo", "src/main/java/Foo.java");
    assertThat(filters.accept(inputFile)).isTrue();
  }

  @Test
  public void at_least_one_filter() throws Exception {
    DeprecatedFileFilters filters = new DeprecatedFileFilters(new FileSystemFilter[] {filter});

    File basedir = temp.newFolder();
    File file = new File(basedir, "src/main/java/Foo.java");
    InputFile inputFile = new DefaultInputFile("foo", "src/main/java/Foo.java")
      .setModuleBaseDir(basedir.toPath())
      .setType(InputFile.Type.MAIN);
    when(filter.accept(eq(file), any(DeprecatedFileFilters.DeprecatedContext.class))).thenReturn(false);

    assertThat(filters.accept(inputFile)).isFalse();

    ArgumentCaptor<DeprecatedFileFilters.DeprecatedContext> argument = ArgumentCaptor.forClass(DeprecatedFileFilters.DeprecatedContext.class);
    verify(filter).accept(eq(file), argument.capture());

    DeprecatedFileFilters.DeprecatedContext context = argument.getValue();
    assertThat(context.canonicalPath()).isEqualTo(FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
    assertThat(context.relativePath()).isEqualTo("src/main/java/Foo.java");
    assertThat(context.type()).isEqualTo(FileType.MAIN);
  }
}

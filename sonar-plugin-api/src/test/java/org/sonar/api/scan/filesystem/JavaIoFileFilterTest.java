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
package org.sonar.api.scan.filesystem;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JavaIoFileFilterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_wrap_java_io_filefilter() throws IOException {
    IOFileFilter javaIoFilter = mock(IOFileFilter.class);
    JavaIoFileFilter filter = JavaIoFileFilter.create(javaIoFilter);

    File file = temp.newFile();
    filter.accept(file, mock(FileFilter.Context.class));

    verify(javaIoFilter).accept(file);
  }
}

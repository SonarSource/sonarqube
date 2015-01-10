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
package org.sonar.api.utils;

import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0
 */
public class PathUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testSanitize() throws Exception {
    assertThat(PathUtils.sanitize("foo/bar/..")).isEqualTo("foo/");
    assertThat(PathUtils.sanitize("C:\\foo\\..\\bar")).isEqualTo("C:/bar");
  }

  @Test
  public void testCanonicalPath_unchecked_exception() throws Exception {
    File file = mock(File.class);
    when(file.getCanonicalPath()).thenThrow(new IOException());

    try {
      PathUtils.canonicalPath(file);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
  }

  @Test
  public void testCanonicalPath() throws Exception {
    File file = temp.newFile();
    String path = PathUtils.canonicalPath(file);
    assertThat(path).isEqualTo(FilenameUtils.separatorsToUnix(file.getCanonicalPath()));
    assertThat(PathUtils.canonicalPath(null)).isNull();
  }

  @Test
  public void haveFunGetCoverage() throws Exception {
    // does not fail
    new PathUtils();
  }
}

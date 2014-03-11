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

import com.google.common.collect.Iterators;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.fest.assertions.Assertions.assertThat;

public class ZipUtilsTest {

  @Test
  public void shouldZipDirectory() throws IOException {
    File foo = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldZipDirectory/foo.txt"));
    File dir = foo.getParentFile();
    File zip = new File("target/tmp/shouldZipDirectory.zip");

    ZipUtils.zipDir(dir, zip);

    assertThat(zip).exists();
    assertThat(zip.length()).isGreaterThan(1L);
    Iterator<? extends ZipEntry> zipEntries = Iterators.forEnumeration(new ZipFile(zip).entries());
    assertThat(zipEntries).hasSize(4);
  }

  @Test
  public void shouldUnzipFile() throws IOException {
    File zip = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldUnzipFile.zip"));
    File toDir = new File("target/tmp/shouldUnzipFile/");
    ZipUtils.unzip(zip, toDir);
    assertThat(toDir.list()).hasSize(3);
  }

}

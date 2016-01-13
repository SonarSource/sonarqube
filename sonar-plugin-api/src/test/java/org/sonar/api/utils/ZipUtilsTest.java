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
package org.sonar.api.utils;

import com.google.common.collect.Iterators;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldZipDirectory() throws IOException {
    File foo = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldZipDirectory/foo.txt"));
    File dir = foo.getParentFile();
    File zip = temp.newFile();

    ZipUtils.zipDir(dir, zip);

    assertThat(zip).exists().isFile();
    assertThat(zip.length()).isGreaterThan(1L);
    Iterator<? extends ZipEntry> zipEntries = Iterators.forEnumeration(new ZipFile(zip).entries());
    assertThat(zipEntries).hasSize(4);

    File unzipDir = temp.newFolder();
    ZipUtils.unzip(zip, unzipDir);
    assertThat(new File(unzipDir, "bar.txt")).exists().isFile();
    assertThat(new File(unzipDir, "foo.txt")).exists().isFile();
    assertThat(new File(unzipDir, "dir1/hello.properties")).exists().isFile();
  }

  @Test
  public void shouldUnzipFile() throws IOException {
    File zip = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldUnzipFile.zip"));
    File toDir = temp.newFolder();
    ZipUtils.unzip(zip, toDir);
    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void should_unzip_stream_file() throws Exception {
    InputStream zip = getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldUnzipFile.zip").openStream();
    File toDir = temp.newFolder();
    ZipUtils.unzip(zip, toDir);
    assertThat(toDir.list()).hasSize(3);
  }
}

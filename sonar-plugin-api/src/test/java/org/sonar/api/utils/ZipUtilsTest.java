/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipUtilsTest {

  @Test
  public void shouldZipDirectory() throws IOException {
    File foo = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldZipDirectory/foo.txt"));
    File dir = foo.getParentFile();
    File zip = new File("target/tmp/shouldZipDirectory.zip");

    ZipUtils.zipDir(dir, zip);

    assertThat(zip.exists(), is(true));
    assertThat(zip.length(), greaterThan(1l));
    assertThat(CollectionUtils.size(new ZipFile(zip).entries()), is(4));
  }

  @Test
  public void shouldUnzipFile() throws IOException {
    File zip = FileUtils.toFile(getClass().getResource("/org/sonar/api/utils/ZipUtilsTest/shouldUnzipFile.zip"));
    File toDir = new File("target/tmp/shouldUnzipFile/");
    ZipUtils.unzip(zip, toDir);
    assertThat(toDir.list().length, is(3));
  }

}

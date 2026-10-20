/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ZipPackagingSupportTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void findExecutableJarEntry_returns_the_single_plugin_jar_entry() throws Exception {
    File zip = writeZip("plugin/my-plugin-1.0.jar", "resources/foo.txt");

    try (ZipFile zipFile = new ZipFile(zip)) {
      ZipEntry entry = ZipPackagingSupport.findExecutableJarEntry(zipFile);
      assertThat(entry.getName()).isEqualTo("plugin/my-plugin-1.0.jar");
    }
  }

  @Test
  public void findExecutableJarEntry_fails_when_no_entry_matches() throws Exception {
    File zip = writeZip("resources/foo.txt");

    try (ZipFile zipFile = new ZipFile(zip)) {
      assertThatThrownBy(() -> ZipPackagingSupport.findExecutableJarEntry(zipFile))
        .isInstanceOf(MessageException.class)
        .hasMessageContaining("found 0");
    }
  }

  @Test
  public void findExecutableJarEntry_fails_when_several_entries_match() throws Exception {
    File zip = writeZip("plugin/a.jar", "plugin/b.jar");

    try (ZipFile zipFile = new ZipFile(zip)) {
      assertThatThrownBy(() -> ZipPackagingSupport.findExecutableJarEntry(zipFile))
        .isInstanceOf(MessageException.class)
        .hasMessageContaining("found 2");
    }
  }

  private File writeZip(String... entryNames) throws Exception {
    File zip = temp.newFile("plugin.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
      for (String entryName : entryNames) {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(entryName.getBytes());
        zos.closeEntry();
      }
    }
    return zip;
  }
}

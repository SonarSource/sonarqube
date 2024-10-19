/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginFilesAndMd5Test {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void getters() throws IOException {
    File jarFile = temp.newFile();
    Files.write(jarFile.toPath(), "f1".getBytes(StandardCharsets.UTF_8));
    File jarFileCompressed = temp.newFile();

    Files.write(jarFileCompressed.toPath(), "f1compressed".getBytes(StandardCharsets.UTF_8));

    PluginFilesAndMd5.FileAndMd5 jar = new PluginFilesAndMd5.FileAndMd5(jarFile);
    PluginFilesAndMd5.FileAndMd5 jarCompressed = new PluginFilesAndMd5.FileAndMd5(jarFileCompressed);

    PluginFilesAndMd5 underTest = new PluginFilesAndMd5(jar, jarCompressed);

    assertThat(underTest.getCompressedJar().getFile()).isEqualTo(jarFileCompressed);
    assertThat(underTest.getCompressedJar().getMd5()).isEqualTo("a0d076c0fc9f11ec68740fed5aa3ce38");

    assertThat(underTest.getLoadedJar().getFile()).isEqualTo(jarFile);
    assertThat(underTest.getLoadedJar().getMd5()).isEqualTo("bd19836ddb62c11c55ab251ccaca5645");
  }

  @Test
  public void fail_if_cant_get_md5() throws IOException {
    File jarFile = new File("nonexisting");
    Assert.assertThrows("Fail to compute md5", IllegalStateException.class, () -> new PluginFilesAndMd5.FileAndMd5(jarFile));
  }
}

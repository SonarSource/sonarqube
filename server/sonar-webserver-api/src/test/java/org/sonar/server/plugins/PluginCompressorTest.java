/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.plugins.PluginCompressor.PROPERTY_PLUGIN_COMPRESSION_ENABLE;

public class PluginCompressorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings = new MapSettings();

  @Before
  public void setUp() throws IOException {
    Path targetFolder = temp.newFolder("target").toPath();
    Path targetJarPath = targetFolder.resolve("test.jar");
    Files.createFile(targetJarPath);
  }

  @Test
  public void compress_jar_if_compression_enabled() throws IOException {
    File jar = touch(temp.newFolder(), "sonar-foo-plugin.jar");
    // the JAR is copied somewhere else in order to be loaded by classloaders
    File loadedJar = touch(temp.newFolder(), "sonar-foo-plugin.jar");

    settings.setProperty(PROPERTY_PLUGIN_COMPRESSION_ENABLE, true);
    PluginCompressor underTest = new PluginCompressor(settings.asConfig());

    PluginFilesAndMd5 installedPlugin = underTest.compress("foo", jar, loadedJar);
    assertThat(installedPlugin.getLoadedJar().getFile().toPath()).isEqualTo(loadedJar.toPath());
    assertThat(installedPlugin.getCompressedJar().getFile())
      .exists()
      .isFile()
      .hasName("sonar-foo-plugin.pack.gz")
      .hasParent(loadedJar.getParentFile());
  }

  @Test
  public void dont_compress_jar_if_compression_disable() throws IOException {
    File jar = touch(temp.newFolder(), "sonar-foo-plugin.jar");
    // the JAR is copied somewhere else in order to be loaded by classloaders
    File loadedJar = touch(temp.newFolder(), "sonar-foo-plugin.jar");

    settings.setProperty(PROPERTY_PLUGIN_COMPRESSION_ENABLE, false);
    PluginCompressor underTest = new PluginCompressor(settings.asConfig());

    PluginFilesAndMd5 installedPlugin = underTest.compress("foo", jar, loadedJar);
    assertThat(installedPlugin.getLoadedJar().getFile().toPath()).isEqualTo(loadedJar.toPath());
    assertThat(installedPlugin.getCompressedJar()).isNull();
    assertThat(installedPlugin.getLoadedJar().getFile().getParentFile().listFiles()).containsOnly(loadedJar);
  }

  @Test
  public void copy_and_use_existing_packed_jar_if_compression_enabled() throws IOException {
    File jar = touch(temp.newFolder(), "sonar-foo-plugin.jar");
    File packedJar = touch(jar.getParentFile(), "sonar-foo-plugin.pack.gz");
    // the JAR is copied somewhere else in order to be loaded by classloaders
    File loadedJar = touch(temp.newFolder(), "sonar-foo-plugin.jar");

    settings.setProperty(PROPERTY_PLUGIN_COMPRESSION_ENABLE, true);
    PluginCompressor underTest = new PluginCompressor(settings.asConfig());

    PluginFilesAndMd5 installedPlugin = underTest.compress("foo", jar, loadedJar);
    assertThat(installedPlugin.getLoadedJar().getFile().toPath()).isEqualTo(loadedJar.toPath());
    assertThat(installedPlugin.getCompressedJar().getFile())
      .exists()
      .isFile()
      .hasName(packedJar.getName())
      .hasParent(loadedJar.getParentFile())
      .hasSameTextualContentAs(packedJar);
  }

  private static File touch(File dir, String filename) throws IOException {
    File file = new File(dir, filename);
    FileUtils.write(file, RandomStringUtils.random(10), StandardCharsets.UTF_8);
    return file;
  }

  //
  // @Test
  // public void should_use_deployed_packed_file() throws IOException {
  // Path packedPath = sourceFolder.resolve("test.pack.gz");
  // Files.write(packedPath, new byte[] {1, 2, 3});
  //
  // settings.setProperty(PROPERTY_PLUGIN_COMPRESSION_ENABLE, true);
  // underTest = new PluginFileSystem(settings.asConfig());
  // underTest.compressJar("key", sourceFolder, targetJarPath);
  //
  // assertThat(Files.list(targetFolder)).containsOnly(targetJarPath, targetFolder.resolve("test.pack.gz"));
  // assertThat(underTest.getPlugins()).hasSize(1);
  // assertThat(underTest.getPlugins().get("key").getFilename()).isEqualTo("test.pack.gz");
  //
  // // check that the file was copied, not generated
  // assertThat(targetFolder.resolve("test.pack.gz")).hasSameContentAs(packedPath);
  // }

}

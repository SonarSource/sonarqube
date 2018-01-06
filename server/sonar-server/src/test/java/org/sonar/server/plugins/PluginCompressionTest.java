/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginCompressionTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings = new MapSettings();
  private Path jarPath;

  private PluginCompression underTest;

  @Before
  public void setUp() throws IOException {
    jarPath = temp.newFile("test.jar").toPath();
  }

  @Test
  public void disable_if_proparty_not_set() throws IOException {
    underTest = new PluginCompression(settings.asConfig());
    underTest.compressJar("key", jarPath);

    assertThat(Files.list(jarPath.getParent())).containsOnly(jarPath);
    assertThat(underTest.getPlugins()).isEmpty();
  }

  @Test
  public void should_compress_plugin() throws IOException {
    settings.setProperty(PluginCompression.PROPERTY_PLUGIN_COMPRESSION_ENABLE, true);
    underTest = new PluginCompression(settings.asConfig());
    underTest.compressJar("key", jarPath);

    assertThat(Files.list(jarPath.getParent())).containsOnly(jarPath, jarPath.getParent().resolve("test.pack.gz"));
    assertThat(underTest.getPlugins()).hasSize(1);
    assertThat(underTest.getPlugins().get("key").getFilename()).isEqualTo("test.pack.gz");
  }
}

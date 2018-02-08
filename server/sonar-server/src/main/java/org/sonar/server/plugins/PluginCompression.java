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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.RemotePluginFile;
import org.sonar.core.util.FileUtils;

public class PluginCompression {
  private static final Logger LOG = Loggers.get(PluginCompression.class);
  static final String PROPERTY_PLUGIN_COMPRESSION_ENABLE = "sonar.pluginsCompression.enable";

  private final Map<String, RemotePluginFile> compressedPlugins = new HashMap<>();
  private final Configuration configuration;

  public PluginCompression(Configuration configuration) {
    this.configuration = configuration;
  }

  public void compressJar(String pluginKey, Path sourceDir, Path targetJarFile) {
    if (configuration.getBoolean(PROPERTY_PLUGIN_COMPRESSION_ENABLE).orElse(false)) {
      Path targetPack200Path = FileUtils.getPack200FilePath(targetJarFile);
      Path sourcePack200Path = sourceDir.resolve(targetPack200Path.getFileName());

      // check if packed file was deployed alongside the jar. If that's the case, use it instead of generating it (SONAR-10395).
      if (Files.isRegularFile(sourcePack200Path)) {
        try {
          LOG.debug("Found pack200: " + sourcePack200Path);
          Files.copy(sourcePack200Path, targetPack200Path);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to copy pack200 file from " + sourcePack200Path + " to " + targetPack200Path, e);
        }
      } else {
        pack200(targetJarFile, targetPack200Path, pluginKey);
      }

      String hash = calculateMd5(targetPack200Path);
      RemotePluginFile compressedPlugin = new RemotePluginFile(targetPack200Path.getFileName().toString(), hash);
      compressedPlugins.put(pluginKey, compressedPlugin);
    }
  }

  public Map<String, RemotePluginFile> getPlugins() {
    return new HashMap<>(compressedPlugins);
  }

  private static String calculateMd5(Path filePath) {
    try (InputStream fis = new BufferedInputStream(Files.newInputStream(filePath))) {
      return DigestUtils.md5Hex(fis);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to compute hash", e);
    }
  }

  private static void pack200(Path jarPath, Path toPath, String pluginKey) {
    Profiler profiler = Profiler.create(LOG);
    profiler.startInfo("Compressing with pack200 plugin: " + pluginKey);
    Pack200.Packer packer = Pack200.newPacker();

    try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarPath)));
      OutputStream out = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(toPath)))) {
      packer.pack(in, out);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to pack200 plugin [%s] '%s' to '%s'", pluginKey, jarPath, toPath), e);
    }
    profiler.stopInfo();
  }
}

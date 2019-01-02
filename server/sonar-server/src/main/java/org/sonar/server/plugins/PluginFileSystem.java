/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPOutputStream;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.InstalledPlugin.FileAndMd5;

import static com.google.common.base.Preconditions.checkState;

@ServerSide
public class PluginFileSystem {

  public static final String PROPERTY_PLUGIN_COMPRESSION_ENABLE = "sonar.pluginsCompression.enable";
  private static final Logger LOG = Loggers.get(PluginFileSystem.class);

  private final Configuration configuration;
  private final Map<String, InstalledPlugin> installedFiles = new HashMap<>();

  public PluginFileSystem(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * @param plugin
   * @param loadedJar the JAR loaded by classloaders. It differs from {@code plugin.getJarFile()}
   *                  which is the initial location of JAR as seen by users
   */
  public void addInstalledPlugin(PluginInfo plugin, File loadedJar) {
    checkState(!installedFiles.containsKey(plugin.getKey()), "Plugin %s is already loaded", plugin.getKey());
    checkState(loadedJar.exists(), "loadedJar does not exist: %s", loadedJar);

    Optional<File> compressed = compressJar(plugin, loadedJar);
    InstalledPlugin installedFile = new InstalledPlugin(
      plugin,
      new FileAndMd5(loadedJar),
      compressed.map(FileAndMd5::new).orElse(null));
    installedFiles.put(plugin.getKey(), installedFile);
  }

  public Optional<InstalledPlugin> getInstalledPlugin(String pluginKey) {
    return Optional.ofNullable(installedFiles.get(pluginKey));
  }

  public Collection<InstalledPlugin> getInstalledFiles() {
    return installedFiles.values();
  }

  private Optional<File> compressJar(PluginInfo plugin, File jar) {
    if (!configuration.getBoolean(PROPERTY_PLUGIN_COMPRESSION_ENABLE).orElse(false)) {
      return Optional.empty();
    }

    Path targetPack200 = getPack200Path(jar.toPath());
    Path sourcePack200Path = getPack200Path(plugin.getNonNullJarFile().toPath());

    // check if packed file was deployed alongside the jar. If that's the case, use it instead of generating it (SONAR-10395).
    if (sourcePack200Path.toFile().exists()) {
      try {
        LOG.debug("Found pack200: " + sourcePack200Path);
        Files.copy(sourcePack200Path, targetPack200);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to copy pack200 file from " + sourcePack200Path + " to " + targetPack200, e);
      }
    } else {
      pack200(jar.toPath(), targetPack200, plugin.getKey());
    }
    return Optional.of(targetPack200.toFile());
  }

  private static void pack200(Path jarPath, Path toPack200Path, String pluginKey) {
    Profiler profiler = Profiler.create(LOG);
    profiler.startInfo("Compressing plugin " + pluginKey + " [pack200]");

    try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarPath)));
      OutputStream out = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(toPack200Path)))) {
      Pack200.newPacker().pack(in, out);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to pack200 plugin [%s] '%s' to '%s'", pluginKey, jarPath, toPack200Path), e);
    }
    profiler.stopInfo();
  }

  private static Path getPack200Path(Path jar) {
    String jarFileName = jar.getFileName().toString();
    String filename = jarFileName.substring(0, jarFileName.length() - 3) + "pack.gz";
    return jar.resolveSibling(filename);
  }
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.sonar.core.platform.PluginInfo;

import static java.util.Objects.requireNonNull;

@Immutable
public class InstalledPlugin {
  private final PluginInfo plugin;
  private final FileAndMd5 loadedJar;
  @Nullable
  private final FileAndMd5 compressedJar;

  public InstalledPlugin(PluginInfo plugin, FileAndMd5 loadedJar, @Nullable FileAndMd5 compressedJar) {
    this.plugin = requireNonNull(plugin);
    this.loadedJar = requireNonNull(loadedJar);
    this.compressedJar = compressedJar;
  }

  public PluginInfo getPluginInfo() {
    return plugin;
  }

  public FileAndMd5 getLoadedJar() {
    return loadedJar;
  }

  @Nullable
  public FileAndMd5 getCompressedJar() {
    return compressedJar;
  }

  @Immutable
  public static final class FileAndMd5 {
    private final File file;
    private final String md5;

    public FileAndMd5(File file) {
      try (InputStream fis = FileUtils.openInputStream(file)) {
        this.file = file;
        this.md5 = DigestUtils.md5Hex(fis);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to compute md5 of " + file, e);
      }
    }

    public File getFile() {
      return file;
    }

    public String getMd5() {
      return md5;
    }
  }
}

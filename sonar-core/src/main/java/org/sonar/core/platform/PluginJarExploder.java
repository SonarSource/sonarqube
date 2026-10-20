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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.io.FileUtils.listFiles;

public abstract class PluginJarExploder {

  protected static final String LIB_RELATIVE_PATH_IN_JAR = "META-INF/lib";

  public abstract ExplodedPlugin explode(PluginInfo plugin);

  protected Predicate<ZipEntry> newLibFilter() {
    return ze -> ze.getName().startsWith(LIB_RELATIVE_PATH_IN_JAR);
  }

  /**
   * Copies the plugin's scanner-side jar into {@code toDir}, ready to be classloaded, and returns
   * that file. For a plain {@code .jar} plugin, this is a verbatim copy of the installed file,
   * keeping its name. For a ZIP-packaged plugin, the installed file is the outer zip, so its
   * nested {@code plugin/*.jar} entry is extracted instead and keeps *that entry's* name — not
   * the outer zip's — since that name is what's reported downstream as the plugin's filename.
   */
  protected File extractExecutableJar(PluginInfo plugin, File toDir) throws IOException {
    FileUtils.forceMkdir(toDir);
    if (!plugin.isZipPackaging()) {
      File target = new File(toDir, plugin.getNonNullJarFile().getName());
      FileUtils.copyFile(plugin.getNonNullJarFile(), target);
      return target;
    }
    try (ZipFile zip = new ZipFile(plugin.getNonNullJarFile())) {
      ZipEntry executableJarEntry = ZipPackagingSupport.findExecutableJarEntry(zip);
      File target = safeTarget(toDir, StringUtils.substringAfterLast(executableJarEntry.getName(), "/"));
      Files.copy(zip.getInputStream(executableJarEntry), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return target;
    }
  }

  /**
   * Resolves {@code entryFileName} against {@code toDir}, rejecting it if the result would not
   * stay inside {@code toDir} (zip slip protection).
   */
  private static File safeTarget(File toDir, String entryFileName) throws IOException {
    File target = new File(toDir, entryFileName);
    String canonicalDir = toDir.getCanonicalPath() + File.separator;
    if (!target.getCanonicalPath().startsWith(canonicalDir)) {
      throw new IOException("Plugin archive entry is outside of the target directory: " + entryFileName);
    }
    return target;
  }

  protected ExplodedPlugin explodeFromUnzippedDir(PluginInfo pluginInfo, File jarFile, File unzippedDir) {
    File libDir = new File(unzippedDir, PluginJarExploder.LIB_RELATIVE_PATH_IN_JAR);
    Collection<File> libs;
    if (libDir.isDirectory() && libDir.exists()) {
      libs = listFiles(libDir, null, false);
    } else {
      libs = Collections.emptyList();
    }
    return new ExplodedPlugin(pluginInfo, pluginInfo.getKey(), jarFile, libs);
  }
}

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
package org.sonar.core.platform;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;

import static org.apache.commons.io.FileUtils.listFiles;

public abstract class PluginJarExploder {

  protected static final String LIB_RELATIVE_PATH_IN_JAR = "META-INF/lib";

  public abstract ExplodedPlugin explode(PluginInfo info);

  protected Predicate<ZipEntry> newLibFilter() {
    return ze -> ze.getName().startsWith(LIB_RELATIVE_PATH_IN_JAR);
  }

  protected ExplodedPlugin explodeFromUnzippedDir(String pluginKey, File jarFile, File unzippedDir) {
    File libDir = new File(unzippedDir, PluginJarExploder.LIB_RELATIVE_PATH_IN_JAR);
    Collection<File> libs;
    if (libDir.isDirectory() && libDir.exists()) {
      libs = listFiles(libDir, null, false);
    } else {
      libs = Collections.emptyList();
    }
    return new ExplodedPlugin(pluginKey, jarFile, libs);
  }
}

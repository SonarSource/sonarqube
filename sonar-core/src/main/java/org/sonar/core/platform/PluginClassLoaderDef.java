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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.classloader.Mask;

/**
 * Temporary information about the classLoader to be created for a plugin (or a group of plugins).
 */
class PluginClassLoaderDef {

  private final String basePluginKey;
  private final Map<String, String> mainClassesByPluginKey = new HashMap<>();
  private final List<File> files = new ArrayList<>();
  private final Mask mask = new Mask();
  private boolean selfFirstStrategy = false;

  PluginClassLoaderDef(String basePluginKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(basePluginKey));
    this.basePluginKey = basePluginKey;
  }

  String getBasePluginKey() {
    return basePluginKey;
  }

  List<File> getFiles() {
    return files;
  }

  void addFiles(Collection<File> f) {
    this.files.addAll(f);
  }

  Mask getExportMask() {
    return mask;
  }

  boolean isSelfFirstStrategy() {
    return selfFirstStrategy;
  }

  void setSelfFirstStrategy(boolean selfFirstStrategy) {
    this.selfFirstStrategy = selfFirstStrategy;
  }

  Map<String, String> getMainClassesByPluginKey() {
    return mainClassesByPluginKey;
  }

  void addMainClass(String pluginKey, @Nullable String mainClass) {
    if (!Strings.isNullOrEmpty(mainClass)) {
      mainClassesByPluginKey.put(pluginKey, mainClass);
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PluginClassLoaderDef that = (PluginClassLoaderDef) o;
    return basePluginKey.equals(that.basePluginKey);
  }

  @Override
  public int hashCode() {
    return basePluginKey.hashCode();
  }
}

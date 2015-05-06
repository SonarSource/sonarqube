/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.classloader.Mask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Information about the classloader to be created for a set of plugins.
 */
class ClassloaderDef {

  private final String basePluginKey;
  private final Map<String, String> mainClassesByPluginKey = new HashMap<>();
  private final List<File> files = new ArrayList<>();
  private final Mask mask = new Mask();
  private boolean selfFirstStrategy = false;
  private ClassLoader classloader = null;

  ClassloaderDef(String basePluginKey) {
    Preconditions.checkNotNull(basePluginKey);
    this.basePluginKey = basePluginKey;
  }

  String getBasePluginKey() {
    return basePluginKey;
  }

  Map<String, String> getMainClassesByPluginKey() {
    return mainClassesByPluginKey;
  }

  List<File> getFiles() {
    return files;
  }

  Mask getMask() {
    return mask;
  }

  boolean isSelfFirstStrategy() {
    return selfFirstStrategy;
  }

  void setSelfFirstStrategy(boolean selfFirstStrategy) {
    this.selfFirstStrategy = selfFirstStrategy;
  }

  /**
   * Returns the newly created classloader. Throws an exception
   * if null, for example because called before {@link #setBuiltClassloader(ClassLoader)}
   */
  ClassLoader getBuiltClassloader() {
    Preconditions.checkState(classloader != null);
    return classloader;
  }

  void setBuiltClassloader(ClassLoader c) {
    this.classloader = c;
  }

  void addFiles(Collection<File> c) {
    this.files.addAll(c);
  }

  void addMainClass(String pluginKey, @Nullable String mainClass) {
    if (!Strings.isNullOrEmpty(mainClass)) {
      mainClassesByPluginKey.put(pluginKey, mainClass);
    }
  }
}

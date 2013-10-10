/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.BatchComponent;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

import java.util.Set;

/**
 * Cache of all files. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
public class InputFileCache implements BatchComponent {

  // module key -> path -> InputFile
  private final Cache<String, InputFile> cache;

  public InputFileCache(Caches caches) {
    cache = caches.createCache("inputFiles");
  }

  public Iterable<InputFile> byModule(String moduleKey) {
    return cache.values(moduleKey);
  }

  public InputFileCache removeModule(String moduleKey) {
    cache.clear(moduleKey);
    return this;
  }

  public InputFileCache remove(String moduleKey, String relativePath) {
    cache.remove(moduleKey, relativePath);
    return this;
  }

  public Iterable<InputFile> all() {
    return cache.allValues();
  }

  public Set<String> fileRelativePaths(String moduleKey) {
    return cache.keySet(moduleKey);
  }

  public boolean containsFile(String moduleKey, String relativePath) {
    return cache.containsKey(moduleKey, relativePath);
  }

  public InputFileCache put(String moduleKey, InputFile file) {
    cache.put(moduleKey, file.relativePath(), file);
    return this;
  }
}

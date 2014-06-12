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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

import javax.annotation.CheckForNull;

/**
 * Cache of all files. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
public class InputFileCache implements BatchComponent {

  // [path type | module key | path] -> InputFile
  // For example:
  // [rel | struts-core | src/main/java/Action.java] -> InputFile
  // [rel | struts-core | src/main/java/Filter.java] -> InputFile
  // [abs | struts-core | /absolute/path/to/src/main/java/Action.java] -> InputFile
  // [abs | struts-core | /absolute/path/to/src/main/java/Filter.java] -> InputFile
  private final Cache<InputFile> cache;

  public InputFileCache(Caches caches) {
    cache = caches.createCache("inputFiles");
  }

  public Iterable<InputFile> all() {
    return cache.values();
  }

  public Iterable<InputFile> byModule(String moduleKey) {
    return cache.values(moduleKey);
  }

  public InputFileCache removeModule(String moduleKey) {
    cache.clear(moduleKey);
    return this;
  }

  public InputFileCache remove(String moduleKey, InputFile inputFile) {
    cache.remove(moduleKey, inputFile.relativePath());
    return this;
  }

  public InputFileCache put(String moduleKey, InputFile inputFile) {
    cache.put(moduleKey, inputFile.relativePath(), inputFile);
    return this;
  }

  @CheckForNull
  public InputFile get(String moduleKey, String relativePath) {
    return cache.get(moduleKey, relativePath);
  }
}

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
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

import javax.annotation.CheckForNull;

/**
 * Cache of all files. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
public class InputPathCache implements BatchComponent {

  private static final String DIR = "DIR";
  private static final String FILE = "FILE";
  // [module key | type | path] -> InputPath
  // For example:
  // [struts-core | FILE | src/main/java/Action.java] -> InputFile
  // [struts-core | FILE | src/main/java/Filter.java] -> InputFile
  // [struts-core | DIR | src/main/java] -> InputDir
  private final Cache<InputPath> cache;

  public InputPathCache(Caches caches) {
    caches.registerValueCoder(DeprecatedDefaultInputFile.class, new DefaultInputFileValueCoder());
    cache = caches.createCache("inputFiles");
  }

  public Iterable<InputPath> all() {
    return cache.values();
  }

  public Iterable<InputFile> filesByModule(String moduleKey) {
    return (Iterable) cache.values(moduleKey, FILE);
  }

  public Iterable<InputDir> dirsByModule(String moduleKey) {
    return (Iterable) cache.values(moduleKey, DIR);
  }

  public InputPathCache removeModule(String moduleKey) {
    cache.clear(moduleKey);
    return this;
  }

  public InputPathCache remove(String moduleKey, InputFile inputFile) {
    cache.remove(moduleKey, FILE, inputFile.relativePath());
    return this;
  }

  public InputPathCache remove(String moduleKey, InputDir inputDir) {
    cache.remove(moduleKey, DIR, inputDir.relativePath());
    return this;
  }

  public InputPathCache put(String moduleKey, InputFile inputFile) {
    cache.put(moduleKey, FILE, inputFile.relativePath(), inputFile);
    return this;
  }

  public InputPathCache put(String moduleKey, InputDir inputDir) {
    cache.put(moduleKey, DIR, inputDir.relativePath(), inputDir);
    return this;
  }

  @CheckForNull
  public InputFile getFile(String moduleKey, String relativePath) {
    return (InputFile) cache.get(moduleKey, FILE, relativePath);
  }

  @CheckForNull
  public InputDir getDir(String moduleKey, String relativePath) {
    return (InputDir) cache.get(moduleKey, DIR, relativePath);
  }

  @CheckForNull
  public InputPath getInputPath(BatchResource component) {
    if (component.isFile()) {
      return getFile(component.parent().parent().resource().getEffectiveKey(), component.resource().getPath());
    } else if (component.isDir()) {
      return getDir(component.parent().parent().resource().getEffectiveKey(), component.resource().getPath());
    }
    return null;
  }

}

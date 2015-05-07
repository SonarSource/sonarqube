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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.batch.index.BatchResource;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Cache of all files and dirs. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
@BatchSide
public class InputPathCache {

  private final Map<String, SortedMap<String, InputFile>> inputFileCache = new LinkedHashMap<>();
  private final Map<String, SortedMap<String, InputDir>> inputDirCache = new LinkedHashMap<>();

  public Iterable<InputFile> allFiles() {
    return Iterables.concat(Iterables.transform(inputFileCache.values(), new Function<Map<String, InputFile>, Collection<InputFile>>() {
      @Override
      public Collection<InputFile> apply(Map<String, InputFile> input) {
        return input.values();
      }
    }));
  }

  public Iterable<InputDir> allDirs() {
    return Iterables.concat(Iterables.transform(inputDirCache.values(), new Function<Map<String, InputDir>, Collection<InputDir>>() {
      @Override
      public Collection<InputDir> apply(Map<String, InputDir> input) {
        return input.values();
      }
    }));
  }

  public Iterable<InputFile> filesByModule(String moduleKey) {
    if (inputFileCache.containsKey(moduleKey)) {
      return inputFileCache.get(moduleKey).values();
    }
    return Collections.emptyList();
  }

  public Iterable<InputDir> dirsByModule(String moduleKey) {
    if (inputDirCache.containsKey(moduleKey)) {
      return inputDirCache.get(moduleKey).values();
    }
    return Collections.emptyList();
  }

  public InputPathCache removeModule(String moduleKey) {
    inputFileCache.remove(moduleKey);
    inputDirCache.remove(moduleKey);
    return this;
  }

  public InputPathCache remove(String moduleKey, InputFile inputFile) {
    if (inputFileCache.containsKey(moduleKey)) {
      inputFileCache.get(moduleKey).remove(inputFile.relativePath());
    }
    return this;
  }

  public InputPathCache remove(String moduleKey, InputDir inputDir) {
    if (inputDirCache.containsKey(moduleKey)) {
      inputDirCache.get(moduleKey).remove(inputDir.relativePath());
    }
    return this;
  }

  public InputPathCache put(String moduleKey, InputFile inputFile) {
    if (!inputFileCache.containsKey(moduleKey)) {
      inputFileCache.put(moduleKey, new TreeMap<String, InputFile>());
    }
    inputFileCache.get(moduleKey).put(inputFile.relativePath(), inputFile);
    return this;
  }

  public InputPathCache put(String moduleKey, InputDir inputDir) {
    if (!inputDirCache.containsKey(moduleKey)) {
      inputDirCache.put(moduleKey, new TreeMap<String, InputDir>());
    }
    inputDirCache.get(moduleKey).put(inputDir.relativePath(), inputDir);
    return this;
  }

  @CheckForNull
  public InputFile getFile(String moduleKey, String relativePath) {
    if (inputFileCache.containsKey(moduleKey)) {
      return inputFileCache.get(moduleKey).get(relativePath);
    }
    return null;
  }

  @CheckForNull
  public InputDir getDir(String moduleKey, String relativePath) {
    if (inputDirCache.containsKey(moduleKey)) {
      return inputDirCache.get(moduleKey).get(relativePath);
    }
    return null;
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

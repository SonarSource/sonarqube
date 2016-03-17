/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;

import javax.annotation.CheckForNull;

/**
 * Cache of all files and dirs. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
@BatchSide
public class InputPathCache {

  private final Table<String, String, InputFile> inputFileCache = TreeBasedTable.create();
  private final Table<String, String, InputDir> inputDirCache = TreeBasedTable.create();

  public Iterable<InputFile> allFiles() {
    return inputFileCache.values();
  }

  public Iterable<InputDir> allDirs() {
    return inputDirCache.values();
  }

  public Iterable<InputFile> filesByModule(String moduleKey) {
    return inputFileCache.row(moduleKey).values();
  }

  public Iterable<InputDir> dirsByModule(String moduleKey) {
    return inputDirCache.row(moduleKey).values();
  }

  public InputPathCache removeModule(String moduleKey) {
    inputFileCache.row(moduleKey).clear();
    inputDirCache.row(moduleKey).clear();
    return this;
  }

  public InputPathCache remove(String moduleKey, InputFile inputFile) {
    inputFileCache.remove(moduleKey, inputFile.relativePath());
    return this;
  }

  public InputPathCache remove(String moduleKey, InputDir inputDir) {
    inputDirCache.remove(moduleKey, inputDir.relativePath());
    return this;
  }

  public InputPathCache put(String moduleKey, InputFile inputFile) {
    inputFileCache.put(moduleKey, inputFile.relativePath(), inputFile);
    return this;
  }

  public InputPathCache put(String moduleKey, InputDir inputDir) {
    inputDirCache.put(moduleKey, inputDir.relativePath(), inputDir);
    return this;
  }

  @CheckForNull
  public InputFile getFile(String moduleKey, String relativePath) {
    return inputFileCache.get(moduleKey, relativePath);
  }

  @CheckForNull
  public InputDir getDir(String moduleKey, String relativePath) {
    return inputDirCache.get(moduleKey, relativePath);
  }

}

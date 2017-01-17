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
package org.sonar.scanner.scan.filesystem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * Store of all files and dirs. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
@ScannerSide
public class InputComponentStore {

  private final Table<String, String, InputFile> inputFileCache = TreeBasedTable.create();
  private final Table<String, String, InputDir> inputDirCache = TreeBasedTable.create();
  private final Map<String, InputModule> inputModuleCache = new HashMap<>();
  private final Map<String, InputComponent> inputComponents = new HashMap<>();
  private InputModule root;

  public Collection<InputComponent> all() {
    return inputComponents.values();
  }

  public Iterable<InputFile> allFiles() {
    return inputFileCache.values();
  }

  public Iterable<InputDir> allDirs() {
    return inputDirCache.values();
  }

  public InputComponent getByKey(String key) {
    return inputComponents.get(key);
  }

  public void setRoot(InputModule root) {
    this.root = root;
  }

  @CheckForNull
  public InputModule root() {
    return root;
  }

  public Iterable<InputFile> filesByModule(String moduleKey) {
    return inputFileCache.row(moduleKey).values();
  }

  public Iterable<InputDir> dirsByModule(String moduleKey) {
    return inputDirCache.row(moduleKey).values();
  }

  public InputComponentStore removeModule(String moduleKey) {
    inputFileCache.row(moduleKey).clear();
    inputDirCache.row(moduleKey).clear();
    return this;
  }

  public InputComponentStore remove(InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    inputFileCache.remove(file.moduleKey(), inputFile.relativePath());
    return this;
  }

  public InputComponentStore remove(InputDir inputDir) {
    DefaultInputDir dir = (DefaultInputDir) inputDir;
    inputDirCache.remove(dir.moduleKey(), inputDir.relativePath());
    return this;
  }

  public InputComponentStore put(InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    inputFileCache.put(file.moduleKey(), inputFile.relativePath(), inputFile);
    inputComponents.put(inputFile.key(), inputFile);
    return this;
  }

  public InputComponentStore put(InputDir inputDir) {
    DefaultInputDir dir = (DefaultInputDir) inputDir;
    inputDirCache.put(dir.moduleKey(), inputDir.relativePath(), inputDir);
    inputComponents.put(inputDir.key(), inputDir);
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

  @CheckForNull
  public InputModule getModule(String moduleKey) {
    return inputModuleCache.get(moduleKey);
  }

  public void put(InputModule inputModule) {
    inputComponents.put(inputModule.key(), inputModule);
    inputModuleCache.put(inputModule.key(), inputModule);
  }

}

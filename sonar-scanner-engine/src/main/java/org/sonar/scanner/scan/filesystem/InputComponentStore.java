/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileExtensionPredicate;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.scanner.scan.branch.BranchConfiguration;

/**
 * Store of all files and dirs. This cache is shared amongst all project modules. Inclusion and
 * exclusion patterns are already applied.
 */
@ScannerSide
public class InputComponentStore {

  private final SortedSet<String> globalLanguagesCache = new TreeSet<>();
  private final Map<String, SortedSet<String>> languagesCache = new HashMap<>();
  private final Map<String, InputFile> globalInputFileCache = new HashMap<>();
  private final Table<String, String, InputFile> inputFileCache = TreeBasedTable.create();
  private final Map<String, InputDir> globalInputDirCache = new HashMap<>();
  private final Table<String, String, InputDir> inputDirCache = TreeBasedTable.create();
  // indexed by key with branch
  private final Map<String, InputModule> inputModuleCache = new HashMap<>();
  private final Map<String, InputComponent> inputComponents = new HashMap<>();
  private final SetMultimap<String, InputFile> filesByNameCache = LinkedHashMultimap.create();
  private final SetMultimap<String, InputFile> filesByExtensionCache = LinkedHashMultimap.create();
  private final InputModule root;
  private final BranchConfiguration branchConfiguration;

  public InputComponentStore(DefaultInputModule root, BranchConfiguration branchConfiguration) {
    this.root = root;
    this.branchConfiguration = branchConfiguration;
    this.put(root);
  }

  public Collection<InputComponent> all() {
    return inputComponents.values();
  }

  public Iterable<DefaultInputFile> allFilesToPublish() {
    return inputFileCache.values().stream()
      .map(f -> (DefaultInputFile) f)
      .filter(DefaultInputFile::isPublished)
      .filter(f -> (!branchConfiguration.isShortLivingBranch()) || f.status() != Status.SAME)::iterator;
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
    inputFileCache.remove(file.moduleKey(), file.getModuleRelativePath());
    return this;
  }

  public InputComponentStore remove(InputDir inputDir) {
    DefaultInputDir dir = (DefaultInputDir) inputDir;
    inputDirCache.remove(dir.moduleKey(), inputDir.relativePath());
    return this;
  }

  public InputComponentStore put(InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    addToLanguageCache(file);
    inputFileCache.put(file.moduleKey(), file.getModuleRelativePath(), inputFile);
    globalInputFileCache.put(file.getProjectRelativePath(), inputFile);
    inputComponents.put(inputFile.key(), inputFile);
    filesByNameCache.put(inputFile.filename(), inputFile);
    filesByExtensionCache.put(FileExtensionPredicate.getExtension(inputFile), inputFile);
    return this;
  }

  private void addToLanguageCache(DefaultInputFile inputFile) {
    String language = inputFile.language();
    if (language != null) {
      globalLanguagesCache.add(language);
      languagesCache.computeIfAbsent(inputFile.moduleKey(), k -> new TreeSet<>()).add(language);
    }
  }

  public InputComponentStore put(InputDir inputDir) {
    DefaultInputDir dir = (DefaultInputDir) inputDir;
    inputDirCache.put(dir.moduleKey(), inputDir.relativePath(), inputDir);
    // FIXME an InputDir can be already indexed by another module
    globalInputDirCache.put(getProjectRelativePath(dir), inputDir);
    inputComponents.put(inputDir.key(), inputDir);
    return this;
  }

  private String getProjectRelativePath(DefaultInputDir dir) {
    return PathResolver.relativize(getProjectBaseDir(), dir.path()).orElseThrow(() -> new IllegalStateException("Dir " + dir.path() + " should be relative to project baseDir"));
  }

  private Path getProjectBaseDir() {
    return ((DefaultInputModule) root).getBaseDir();
  }

  @CheckForNull
  public InputFile getFile(String moduleKey, String relativePath) {
    return inputFileCache.get(moduleKey, relativePath);
  }

  @CheckForNull
  public InputFile getFile(String relativePath) {
    return globalInputFileCache.get(relativePath);
  }

  @CheckForNull
  public InputDir getDir(String moduleKey, String relativePath) {
    return inputDirCache.get(moduleKey, relativePath);
  }

  @CheckForNull
  public InputDir getDir(String relativePath) {
    return globalInputDirCache.get(relativePath);
  }

  @CheckForNull
  public InputModule getModule(String moduleKeyWithBranch) {
    return inputModuleCache.get(moduleKeyWithBranch);
  }

  public void put(DefaultInputModule inputModule) {
    String key = inputModule.key();
    String keyWithBranch = inputModule.getKeyWithBranch();
    Preconditions.checkNotNull(inputModule);
    Preconditions.checkState(!inputComponents.containsKey(key), "Module '%s' already indexed", key);
    Preconditions.checkState(!inputModuleCache.containsKey(keyWithBranch), "Module '%s' already indexed", keyWithBranch);
    inputComponents.put(key, inputModule);
    inputModuleCache.put(keyWithBranch, inputModule);
  }

  public Iterable<InputFile> getFilesByName(String filename) {
    return filesByNameCache.get(filename);
  }

  public Iterable<InputFile> getFilesByExtension(String extension) {
    return filesByExtensionCache.get(extension);
  }

  public SortedSet<String> getLanguages() {
    return globalLanguagesCache;
  }

  public SortedSet<String> getLanguages(String moduleKey) {
    return languagesCache.getOrDefault(moduleKey, Collections.emptySortedSet());
  }
}

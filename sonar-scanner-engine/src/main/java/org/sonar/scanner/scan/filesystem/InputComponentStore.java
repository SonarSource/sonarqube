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
package org.sonar.scanner.scan.filesystem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.predicates.FileExtensionPredicate;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.sonar.api.utils.Preconditions.checkNotNull;
import static org.sonar.api.utils.Preconditions.checkState;

/**
 * Store of all files and dirs. Inclusion and
 * exclusion patterns are already applied.
 */
public class InputComponentStore extends DefaultFileSystem.Cache {

  private final SortedSet<String> globalLanguagesCache = new TreeSet<>();
  private final Map<String, SortedSet<String>> languagesCache = new HashMap<>();
  private final Map<String, InputFile> globalInputFileCache = new HashMap<>();
  private final Map<String, Map<String, InputFile>> inputFileByModuleCache = new LinkedHashMap<>();
  private final Map<InputFile, String> inputModuleKeyByFileCache = new HashMap<>();
  private final Map<String, DefaultInputModule> inputModuleCache = new HashMap<>();
  private final Map<String, InputComponent> inputComponents = new HashMap<>();
  private final Map<String, Set<InputFile>> filesByNameCache = new HashMap<>();
  private final Map<String, Set<InputFile>> filesByExtensionCache = new HashMap<>();
  private final BranchConfiguration branchConfiguration;

  public InputComponentStore(BranchConfiguration branchConfiguration) {
    this.branchConfiguration = branchConfiguration;
  }

  public Collection<InputComponent> all() {
    return inputComponents.values();
  }

  private Stream<DefaultInputFile> allFilesToPublishStream() {
    return globalInputFileCache.values().stream()
      .map(f -> (DefaultInputFile) f)
      .filter(DefaultInputFile::isPublished);
  }

  public Iterable<DefaultInputFile> allFilesToPublish() {
    return allFilesToPublishStream()::iterator;
  }

  public Iterable<DefaultInputFile> allChangedFilesToPublish() {
    return allFilesToPublishStream()
      .filter(f -> !branchConfiguration.isShortOrPullRequest() || f.status() != InputFile.Status.SAME)::iterator;
  }

  @Override
  public Collection<InputFile> inputFiles() {
    return globalInputFileCache.values();
  }

  public InputComponent getByKey(String key) {
    return inputComponents.get(key);
  }

  public Iterable<InputFile> filesByModule(String moduleKey) {
    return inputFileByModuleCache.getOrDefault(moduleKey, Collections.emptyMap()).values();
  }

  public InputComponentStore put(String moduleKey, InputFile inputFile) {
    DefaultInputFile file = (DefaultInputFile) inputFile;
    addToLanguageCache(moduleKey, file);
    inputFileByModuleCache.computeIfAbsent(moduleKey, x -> new HashMap<>()).put(file.getModuleRelativePath(), inputFile);
    inputModuleKeyByFileCache.put(inputFile, moduleKey);
    globalInputFileCache.put(file.getProjectRelativePath(), inputFile);
    inputComponents.put(inputFile.key(), inputFile);
    filesByNameCache.computeIfAbsent(inputFile.filename(), x -> new LinkedHashSet<>()).add(inputFile);
    filesByExtensionCache.computeIfAbsent(FileExtensionPredicate.getExtension(inputFile), x -> new LinkedHashSet<>()).add(inputFile);
    return this;
  }

  private void addToLanguageCache(String moduleKey, DefaultInputFile inputFile) {
    String language = inputFile.language();
    if (language != null) {
      globalLanguagesCache.add(language);
      languagesCache.computeIfAbsent(moduleKey, k -> new TreeSet<>()).add(language);
    }
  }

  @CheckForNull
  public InputFile getFile(String moduleKey, String relativePath) {
    return inputFileByModuleCache.getOrDefault(moduleKey, Collections.emptyMap())
      .get(relativePath);
  }

  @Override
  @CheckForNull
  public InputFile inputFile(String relativePath) {
    return globalInputFileCache.get(relativePath);
  }

  public DefaultInputModule findModule(DefaultInputFile file) {
    return Optional.ofNullable(inputModuleKeyByFileCache.get(file)).map(inputModuleCache::get)
      .orElseThrow(() -> new IllegalStateException("No modules for file '" + file.toString() + "'"));
  }

  public void put(DefaultInputModule inputModule) {
    String key = inputModule.key();
    checkNotNull(inputModule);
    checkState(!inputComponents.containsKey(key), "Module '%s' already indexed", key);
    checkState(!inputModuleCache.containsKey(key), "Module '%s' already indexed", key);
    inputComponents.put(key, inputModule);
    inputModuleCache.put(key, inputModule);
  }

  @Override
  public Iterable<InputFile> getFilesByName(String filename) {
    return filesByNameCache.getOrDefault(filename, Collections.emptySet());
  }

  @Override
  public Iterable<InputFile> getFilesByExtension(String extension) {
    return filesByExtensionCache.getOrDefault(extension, Collections.emptySet());
  }

  @Override
  public SortedSet<String> languages() {
    return globalLanguagesCache;
  }

  public SortedSet<String> languages(String moduleKey) {
    return languagesCache.getOrDefault(moduleKey, Collections.emptySortedSet());
  }

  public Collection<DefaultInputModule> allModules() {
    return inputModuleCache.values();
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    throw new UnsupportedOperationException();
  }

}

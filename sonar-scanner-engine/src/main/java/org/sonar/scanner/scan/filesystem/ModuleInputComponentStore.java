/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.SensorStrategy;

@ScannerSide
public class ModuleInputComponentStore extends DefaultFileSystem.Cache {

  private final String moduleKey;
  private final InputComponentStore inputComponentStore;
  private final SensorStrategy strategy;

  public ModuleInputComponentStore(InputModule module, InputComponentStore inputComponentStore, SensorStrategy strategy) {
    this.moduleKey = module.key();
    this.inputComponentStore = inputComponentStore;
    this.strategy = strategy;
  }

  @Override
  public Iterable<InputFile> inputFiles() {
    if (strategy.isGlobal()) {
      return inputComponentStore.inputFiles();
    } else {
      return inputComponentStore.filesByModule(moduleKey);
    }
  }

  @Override
  public InputFile inputFile(String relativePath) {
    if (strategy.isGlobal()) {
      return inputComponentStore.inputFile(relativePath);
    } else {
      return inputComponentStore.getFile(moduleKey, relativePath);
    }
  }

  @Override
  public SortedSet<String> languages() {
    if (strategy.isGlobal()) {
      return inputComponentStore.languages();
    } else {
      return inputComponentStore.languages(moduleKey);
    }
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    inputComponentStore.put(moduleKey, inputFile);
  }

  @Override
  public Iterable<InputFile> getFilesByName(String filename) {
    Iterable<InputFile> allFilesByName = inputComponentStore.getFilesByName(filename);
    if (strategy.isGlobal()) {
      return allFilesByName;
    }

    return filterByModule(allFilesByName);
  }

  @Override
  public Iterable<InputFile> getFilesByExtension(String extension) {
    Iterable<InputFile> allFilesByExtension = inputComponentStore.getFilesByExtension(extension);
    if (strategy.isGlobal()) {
      return allFilesByExtension;
    }

    return filterByModule(allFilesByExtension);
  }

  private Iterable<InputFile> filterByModule(Iterable<InputFile> projectInputFiles) {
    Set<InputFile> projectInputFilesSet = StreamSupport.stream(projectInputFiles.spliterator(), false)
      .collect(Collectors.toSet());
    return StreamSupport.stream(inputComponentStore.filesByModule(moduleKey).spliterator(), false)
      .filter(projectInputFilesSet::contains)
      .toList();
  }
}

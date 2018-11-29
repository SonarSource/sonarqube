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

import java.util.SortedSet;
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
    return inputComponentStore.getFilesByName(filename);
  }

  @Override
  public Iterable<InputFile> getFilesByExtension(String extension) {
    return inputComponentStore.getFilesByExtension(extension);
  }
}

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

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;

@ScannerSide
public class ModuleInputComponentStore extends DefaultFileSystem.Cache {

  private final String moduleKey;
  private final InputComponentStore inputComponentStore;

  public ModuleInputComponentStore(InputModule module, InputComponentStore inputComponentStore) {
    this.moduleKey = module.key();
    this.inputComponentStore = inputComponentStore;
  }

  @Override
  public Iterable<InputFile> inputFiles() {
    return inputComponentStore.filesByModule(moduleKey);
  }

  @Override
  public InputFile inputFile(String relativePath) {
    return inputComponentStore.getFile(moduleKey, relativePath);
  }

  @Override
  public InputDir inputDir(String relativePath) {
    return inputComponentStore.getDir(moduleKey, relativePath);
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    inputComponentStore.put(inputFile);
  }

  @Override
  protected void doAdd(InputDir inputDir) {
    inputComponentStore.put(inputDir);
  }

  @Override
  protected void doAdd(InputModule inputModule) {
    inputComponentStore.put(inputModule);
  }

  @Override
  public InputModule module() {
    return inputComponentStore.getModule(moduleKey);
  }
}

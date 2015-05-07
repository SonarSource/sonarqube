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

import org.sonar.api.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;

@BatchSide
public class ModuleInputFileCache extends DefaultFileSystem.Cache {

  private final String moduleKey;
  private final InputPathCache inputPathCache;

  public ModuleInputFileCache(ProjectDefinition projectDef, InputPathCache projectCache) {
    this.moduleKey = projectDef.getKeyWithBranch();
    this.inputPathCache = projectCache;
  }

  @Override
  public Iterable<InputFile> inputFiles() {
    return inputPathCache.filesByModule(moduleKey);
  }

  @Override
  public InputFile inputFile(String relativePath) {
    return inputPathCache.getFile(moduleKey, relativePath);
  }

  @Override
  public InputDir inputDir(String relativePath) {
    return inputPathCache.getDir(moduleKey, relativePath);
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    inputPathCache.put(moduleKey, inputFile);
  }

  @Override
  protected void doAdd(InputDir inputDir) {
    inputPathCache.put(moduleKey, inputDir);
  }
}

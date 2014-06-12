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
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.RelativePathPredicate;
import org.sonar.api.resources.Project;

public class ModuleInputFileCache extends DefaultFileSystem.Cache implements BatchComponent {

  private final String moduleKey;
  private final InputFileCache projectCache;

  public ModuleInputFileCache(Project module, ProjectDefinition projectDef, InputFileCache projectCache) {
    this.moduleKey = module.getKey();
    this.projectCache = projectCache;
  }

  /**
   * Used by scan2
   */
  public ModuleInputFileCache(ProjectDefinition projectDef, InputFileCache projectCache) {
    this.moduleKey = projectDef.getKey();
    this.projectCache = projectCache;
  }

  @Override
  protected Iterable<InputFile> inputFiles() {
    return projectCache.byModule(moduleKey);
  }

  @Override
  protected InputFile inputFile(RelativePathPredicate predicate) {
    return projectCache.get(moduleKey, predicate.path());
  }

  @Override
  protected void doAdd(InputFile inputFile) {
    projectCache.put(moduleKey, inputFile);
  }
}

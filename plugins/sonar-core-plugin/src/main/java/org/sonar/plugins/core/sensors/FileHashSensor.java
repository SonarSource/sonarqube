/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.sensors;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.FileType;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.scan.filesystem.FileHashCache;
import org.sonar.core.source.SnapshotDataType;

import java.io.File;
import java.util.List;

/**
 * This sensor will retrieve hash of each file of the current module and store it in DB
 * in order to compare it during next analysis and see if the file was modified.
 * This is used by the incremental preview mode.
 * @see org.sonar.plugins.core.batch.IncrementalPreviewFilter
 * @since 4.0
 */
public final class FileHashSensor implements Sensor {

  private ModuleFileSystem moduleFileSystem;
  private PathResolver pathResolver;
  private ComponentDataCache componentDataCache;
  private FileHashCache fileHashCache;

  public FileHashSensor(FileHashCache fileHashCache, ModuleFileSystem moduleFileSystem, PathResolver pathResolver, ComponentDataCache componentDataCache) {
    this.fileHashCache = fileHashCache;
    this.moduleFileSystem = moduleFileSystem;
    this.pathResolver = pathResolver;
    this.componentDataCache = componentDataCache;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    StringBuilder fileHashMap = new StringBuilder();
    analyse(fileHashMap, project, FileType.SOURCE);
    analyse(fileHashMap, project, FileType.TEST);
    String fileHashes = fileHashMap.toString();
    if (StringUtils.isNotBlank(fileHashes)) {
      componentDataCache.setStringData(project.getKey(), SnapshotDataType.FILE_HASH.getValue(), fileHashes);
    }
  }

  private void analyse(StringBuilder fileHashMap, Project project, FileType fileType) {
    List<File> files = moduleFileSystem.files(FileQuery.on(fileType).onLanguage(project.getLanguageKey()));
    for (File file : files) {
      String hash = fileHashCache.getCurrentHash(file, moduleFileSystem.sourceCharset());
      fileHashMap.append(pathResolver.relativePath(moduleFileSystem.baseDir(), file)).append("=").append(hash).append("\n");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

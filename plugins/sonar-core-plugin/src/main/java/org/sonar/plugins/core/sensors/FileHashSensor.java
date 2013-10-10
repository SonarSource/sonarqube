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

import com.google.common.collect.Maps;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.scan.filesystem.InputFileCache;
import org.sonar.core.DryRunIncompatible;
import org.sonar.core.source.SnapshotDataTypes;

import java.util.Map;

/**
 * This sensor will retrieve hash of each file of the current module and store it in DB
 * in order to compare it during next analysis and see if the file was modified.
 * This is used by the incremental preview mode.
 *
 * @since 4.0
 */
@DryRunIncompatible
public final class FileHashSensor implements Sensor {

  private final InputFileCache fileCache;
  private final ComponentDataCache componentDataCache;

  public FileHashSensor(InputFileCache fileCache, ComponentDataCache componentDataCache) {
    this.fileCache = fileCache;
    this.componentDataCache = componentDataCache;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    Map<String, String> map = Maps.newHashMap();
    for (InputFile inputFile : fileCache.byModule(project.key())) {
      String hash = inputFile.attribute(InputFile.ATTRIBUTE_HASH);
      if (hash != null) {
        map.put(inputFile.path(), hash);
      }
    }
    if (!map.isEmpty()) {
      String data = KeyValueFormat.format(map);
      componentDataCache.setStringData(project.getKey(), SnapshotDataTypes.FILE_HASHES, data);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

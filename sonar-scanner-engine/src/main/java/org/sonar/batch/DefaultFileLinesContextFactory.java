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
package org.sonar.batch;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.scan.measure.MeasureCache;

public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final SonarIndex index;
  private final SensorContext sensorContext;
  private final MetricFinder metricFinder;
  private final MeasureCache measureCache;
  private final BatchComponentCache scannerComponentCache;

  public DefaultFileLinesContextFactory(SonarIndex index, SensorContext sensorContext, MetricFinder metricFinder, BatchComponentCache scannerComponentCache,
    MeasureCache measureCache) {
    this.index = index;
    this.sensorContext = sensorContext;
    this.metricFinder = metricFinder;
    this.scannerComponentCache = scannerComponentCache;
    this.measureCache = measureCache;
  }

  @Override
  public FileLinesContext createFor(Resource resource) {
    Preconditions.checkArgument(ResourceUtils.isFile(resource));
    // Reload resource in case it use deprecated key
    File file = (File) index.getResource(resource);
    if (file == null) {
      throw new IllegalArgumentException("Unable to find resource " + resource + " in index.");
    }
    return new DefaultFileLinesContext(sensorContext, (InputFile) scannerComponentCache.get(file).inputComponent(), metricFinder, measureCache);
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    return new DefaultFileLinesContext(sensorContext, inputFile, metricFinder, measureCache);
  }

}

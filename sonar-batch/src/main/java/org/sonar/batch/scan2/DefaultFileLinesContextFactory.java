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
package org.sonar.batch.scan2;

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Resource;
import org.sonar.batch.scan.filesystem.InputFileCache;

public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final AnalyzerMeasureCache measureCache;
  private final MetricFinder metricFinder;
  private final ProjectDefinition def;
  private InputFileCache fileCache;

  public DefaultFileLinesContextFactory(InputFileCache fileCache, FileSystem fs, MetricFinder metricFinder, AnalyzerMeasureCache measureCache,
    ProjectDefinition def) {
    this.fileCache = fileCache;
    this.metricFinder = metricFinder;
    this.measureCache = measureCache;
    this.def = def;
  }

  @Override
  public FileLinesContext createFor(Resource model) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    if (fileCache.get(def.getKey(), inputFile.relativePath()) == null) {
      throw new IllegalStateException("InputFile is not indexed: " + inputFile);
    }
    return new DefaultFileLinesContext(metricFinder, measureCache, def.getKey(), inputFile);
  }

}

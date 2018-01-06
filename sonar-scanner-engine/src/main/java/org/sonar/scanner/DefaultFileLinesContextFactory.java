/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.scanner.scan.measure.MeasureCache;

@Immutable
public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final SensorContext sensorContext;
  private final MetricFinder metricFinder;
  private final MeasureCache measureCache;

  public DefaultFileLinesContextFactory(SensorContext sensorContext, MetricFinder metricFinder, MeasureCache measureCache) {
    this.sensorContext = sensorContext;
    this.metricFinder = metricFinder;
    this.measureCache = measureCache;
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    return new DefaultFileLinesContext(sensorContext, inputFile, metricFinder, measureCache);
  }

}

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
package org.sonar.scanner;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

@Immutable
public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final SensorStorage sensorStorage;
  private final MetricFinder metricFinder;

  public DefaultFileLinesContextFactory(SensorStorage sensorStorage, MetricFinder metricFinder) {
    this.sensorStorage = sensorStorage;
    this.metricFinder = metricFinder;
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    return new DefaultFileLinesContext(sensorStorage, inputFile, metricFinder);
  }

}

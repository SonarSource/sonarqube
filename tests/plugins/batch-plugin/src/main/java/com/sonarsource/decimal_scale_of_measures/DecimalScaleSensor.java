/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class DecimalScaleSensor implements Sensor {
  private static final Logger LOG = Loggers.get(DecimalScaleSensor.class);

  public void describe(SensorDescriptor descriptor) {
    // nothing to do
  }

  @Override
  public void execute(SensorContext context) {
    if (context.config().getBoolean(DecimalScaleProperty.KEY).orElse(false)) {
      FilePredicate all = context.fileSystem().predicates().all();
      Iterable<InputFile> files = context.fileSystem().inputFiles(all);
      double value = 0.0001;
      for (InputFile file : files) {
        LOG.info("Value for {}: {}", file, value);
        context.newMeasure()
          .on(file)
          .forMetric(DecimalScaleMetric.definition())
          .withValue(value)
          .save();
        value += 0.0001;
      }
    }

  }
}

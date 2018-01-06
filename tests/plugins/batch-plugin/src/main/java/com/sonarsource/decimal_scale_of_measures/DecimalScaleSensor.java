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
package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class DecimalScaleSensor implements Sensor {
  private static final Logger LOG = Loggers.get(DecimalScaleSensor.class);

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    if (context.settings().getBoolean(DecimalScaleProperty.KEY)) {
      FilePredicate all = context.fileSystem().predicates().all();
      Iterable<InputFile> files = context.fileSystem().inputFiles(all);
      double value = 0.0001;
      for (InputFile file : files) {
        LOG.info("Value for {}: {}", file.relativePath(), value);
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

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
package org.sonar.batch.source;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;

@Phase(name = Phase.Name.PRE)
public final class LinesSensor implements Sensor {

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Lines Sensor");
  }

  @Override
  public void execute(final SensorContext context) {
    FileSystem fs = context.fileSystem();
    for (InputFile f : fs.inputFiles(fs.predicates().hasType(Type.MAIN))) {
      ((DefaultMeasure<Integer>) context.<Integer>newMeasure()
        .on(f)
        .forMetric(CoreMetrics.LINES)
        .withValue(f.lines()))
          .setFromCore()
          .save();
      if (f.language() == null) {
        // As an approximation for files with no language plugin we consider every non blank line as ncloc
        ((DefaultMeasure<Integer>) context.<Integer>newMeasure()
          .on(f)
          .forMetric(CoreMetrics.NCLOC)
          .withValue(((DefaultInputFile) f).nonBlankLines()))
            .save();
        // No test and no coverage on those files
        ((DefaultMeasure<Integer>) context.<Integer>newMeasure()
          .on(f)
          .forMetric(CoreMetrics.LINES_TO_COVER)
          .withValue(0))
            .save();
      }
    }
  }

}

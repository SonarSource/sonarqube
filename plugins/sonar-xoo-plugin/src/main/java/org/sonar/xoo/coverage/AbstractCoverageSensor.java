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
package org.sonar.xoo.coverage;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

public abstract class AbstractCoverageSensor implements Sensor {
  private static final Logger LOG = Loggers.get(AbstractCoverageSensor.class);

  private void processCoverage(InputFile inputFile, SensorContext context) {
    File coverageFile = new File(inputFile.file().getParentFile(), inputFile.file().getName() + getCoverageExtension());
    if (coverageFile.exists()) {
      LOG.debug("Processing " + coverageFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(coverageFile, context.fileSystem().encoding().name());
        NewCoverage coverageBuilder = context.newCoverage()
          .onFile(inputFile)
          .ofType(getCoverageType());
        int lineNumber = 0;
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line)) {
            continue;
          }
          if (line.startsWith("#")) {
            continue;
          }
          try {
            Iterator<String> split = Splitter.on(":").split(line).iterator();
            int lineId = Integer.parseInt(split.next());
            int lineHits = Integer.parseInt(split.next());
            coverageBuilder.lineHits(lineId, lineHits);
            if (split.hasNext()) {
              int conditions = Integer.parseInt(split.next());
              int coveredConditions = Integer.parseInt(split.next());
              coverageBuilder.conditions(lineId, conditions, coveredConditions);
            }
          } catch (Exception e) {
            throw new IllegalStateException("Error processing line " + lineNumber + " of file " + coverageFile.getAbsolutePath(), e);
          }
        }
        coverageBuilder.save();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected abstract String getCoverageExtension();

  protected abstract CoverageType getCoverageType();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name(getSensorName())
      .onlyOnLanguages(Xoo.KEY);
  }

  protected abstract String getSensorName();

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processCoverage(file, context);
    }
  }

}

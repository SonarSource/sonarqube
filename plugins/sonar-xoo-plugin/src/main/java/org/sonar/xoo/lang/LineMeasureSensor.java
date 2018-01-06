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
package org.sonar.xoo.lang;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

/**
 * Parse files *.xoo.measures
 */
public class LineMeasureSensor implements Sensor {

  private static final Logger LOG = Loggers.get(LineMeasureSensor.class);

  private static final String MEASURES_EXTENSION = ".linemeasures";

  private FileLinesContextFactory contextFactory;

  public LineMeasureSensor(FileLinesContextFactory contextFactory) {
    this.contextFactory = contextFactory;
  }

  private void processFileMeasures(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File measureFile = new File(ioFile.getParentFile(), ioFile.getName() + MEASURES_EXTENSION);
    if (measureFile.exists()) {
      LOG.debug("Processing " + measureFile.getAbsolutePath());
      try {
        FileLinesContext linesContext = contextFactory.createFor(inputFile);
        List<String> lines = FileUtils.readLines(measureFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processMeasure(inputFile, linesContext, measureFile, lineNumber, line);
        }
        linesContext.save();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void processMeasure(InputFile inputFile, FileLinesContext context, File measureFile, int lineNumber, String line) {
    try {
      String metricKey = StringUtils.substringBefore(line, ":");
      String value = line.substring(metricKey.length() + 1);
      saveMeasure(context, inputFile, metricKey, KeyValueFormat.parseIntInt(value));
    } catch (Exception e) {
      LOG.error("Error processing line " + lineNumber + " of file " + measureFile.getAbsolutePath(), e);
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + measureFile.getAbsolutePath(), e);
    }
  }

  private void saveMeasure(FileLinesContext context, InputFile xooFile, String metricKey, Map<Integer, Integer> values) {
    for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
      context.setIntValue(metricKey, entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Line Measure Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileMeasures(file, context);
    }
  }
}

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
import java.io.Serializable;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;

/**
 * Parse files *.xoo.measures
 */
public class MeasureSensor implements Sensor {

  private static final Logger LOG = Loggers.get(MeasureSensor.class);

  private static final String MEASURES_EXTENSION = ".measures";

  private MetricFinder metricFinder;

  public MeasureSensor(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  private void processFileMeasures(InputComponent component, File measureFile, SensorContext context) {
    if (measureFile.exists()) {
      LOG.debug("Processing " + measureFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(measureFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processMeasure(component, context, measureFile, lineNumber, line);
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void processMeasure(InputComponent component, SensorContext context, File measureFile, int lineNumber, String line) {
    try {
      String metricKey = StringUtils.substringBefore(line, ":");
      String value = line.substring(metricKey.length() + 1);
      saveMeasure(context, component, metricKey, value);
    } catch (Exception e) {
      LOG.error("Error processing line " + lineNumber + " of file " + measureFile.getAbsolutePath(), e);
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + measureFile.getAbsolutePath(), e);
    }
  }

  private void saveMeasure(SensorContext context, InputComponent component, String metricKey, String value) {
    org.sonar.api.batch.measure.Metric<Serializable> metric = metricFinder.findByKey(metricKey);
    if (metric == null) {
      throw new IllegalStateException("Unknow metric with key: " + metricKey);
    }
    NewMeasure<Serializable> newMeasure = context.newMeasure()
      .forMetric(metric)
      .on(component);
    if (Boolean.class.equals(metric.valueType())) {
      newMeasure.withValue(Boolean.parseBoolean(value));
    } else if (Integer.class.equals(metric.valueType())) {
      newMeasure.withValue(Integer.valueOf(value));
    } else if (Double.class.equals(metric.valueType())) {
      newMeasure.withValue(Double.valueOf(value));
    } else if (String.class.equals(metric.valueType())) {
      newMeasure.withValue(value);
    } else if (Long.class.equals(metric.valueType())) {
      newMeasure.withValue(Long.valueOf(value));
    } else {
      throw new UnsupportedOperationException("Unsupported type :" + metric.valueType());
    }
    newMeasure.save();
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Measure Sensor")
      .onlyOnLanguages(Xoo.KEY, Xoo2.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY, Xoo2.KEY))) {
      File ioFile = file.file();
      File measureFile = new File(ioFile.getParentFile(), ioFile.getName() + MEASURES_EXTENSION);
      processFileMeasures(file, measureFile, context);
    }
    processFileMeasures(context.module(), new File(context.fileSystem().baseDir(), "module" + MEASURES_EXTENSION), context);
  }
}

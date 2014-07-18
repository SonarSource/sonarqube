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
package org.sonar.batch.mediumtest.xoo.plugin.lang;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.analyzer.Analyzer;
import org.sonar.api.batch.analyzer.AnalyzerContext;
import org.sonar.api.batch.analyzer.AnalyzerDescriptor;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasureBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.mediumtest.xoo.plugin.base.Xoo;
import org.sonar.batch.mediumtest.xoo.plugin.base.XooConstants;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Parse files *.xoo.measures
 */
public class MeasureAnalyzer implements Analyzer {

  private static final String MEASURES_EXTENSION = ".measures";

  private MetricFinder metricFinder;

  public MeasureAnalyzer(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  private void processFileMeasures(InputFile inputFile, AnalyzerContext context) {
    File ioFile = inputFile.file();
    File measureFile = new File(ioFile.getParentFile(), ioFile.getName() + MEASURES_EXTENSION);
    if (measureFile.exists()) {
      XooConstants.LOG.debug("Processing " + measureFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(measureFile, context.fileSystem().encoding().name());
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
            String metricKey = StringUtils.substringBefore(line, ":");
            String value = line.substring(metricKey.length() + 1);
            context.addMeasure(createMeasure(context, inputFile, metricKey, value));
          } catch (Exception e) {
            throw new IllegalStateException("Error processing line " + lineNumber + " of file " + measureFile.getAbsolutePath(), e);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private AnalyzerMeasure<?> createMeasure(AnalyzerContext context, InputFile xooFile, String metricKey, String value) {
    org.sonar.api.batch.measure.Metric<Serializable> metric = metricFinder.findByKey(metricKey);
    AnalyzerMeasureBuilder<Serializable> builder = context.measureBuilder()
      .forMetric(metric)
      .onFile(xooFile);
    if (Boolean.class.equals(metric.valueType())) {
      builder.withValue(Boolean.parseBoolean(value));
    } else if (Integer.class.equals(metric.valueType())) {
      builder.withValue(Integer.valueOf(value));
    } else if (Double.class.equals(metric.valueType())) {
      builder.withValue(Double.valueOf(value));
    } else if (String.class.equals(metric.valueType())) {
      builder.withValue(value);
    } else if (Long.class.equals(metric.valueType())) {
      builder.withValue(Long.valueOf(value));
    } else {
      throw new UnsupportedOperationException("Unsupported type :" + metric.valueType());
    }
    return builder.build();
  }

  @Override
  public void describe(AnalyzerDescriptor descriptor) {
    descriptor
      .name("Xoo Measure Analyzer")
      .provides(CoreMetrics.LINES)
      .workOnLanguages(Xoo.KEY)
      .workOnFileTypes(InputFile.Type.MAIN, InputFile.Type.TEST);
  }

  @Override
  public void analyse(AnalyzerContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileMeasures(file, context);
    }
  }
}

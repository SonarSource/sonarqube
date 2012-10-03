/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;

import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfoStore;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class JaCoCoAllTestsSensor implements Sensor {
  private static final String MERGED_EXEC = "target/sonar/merged.exec";

  private final JacocoConfiguration configuration;

  public JaCoCoAllTestsSensor(JacocoConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotBlank(configuration.getItReportPath())
      && project.getAnalysisType().isDynamic(true);
  }

  public void analyse(Project project, SensorContext context) {
    mergeReports(project);

    new AllTestsAnalyzer().analyse(project, context);
  }

  private void mergeReports(Project project) {
    File reportUTs = project.getFileSystem().resolvePath(configuration.getReportPath());
    File reportITs = project.getFileSystem().resolvePath(configuration.getItReportPath());
    File reportAllTests = project.getFileSystem().resolvePath(MERGED_EXEC);
    reportAllTests.getParentFile().mkdirs();

    SessionInfoStore infoStore = new SessionInfoStore();
    ExecutionDataStore dataStore = new ExecutionDataStore();

    loadSourceFiles(infoStore, dataStore, reportUTs, reportITs);

    BufferedOutputStream outputStream = null;
    try {
      outputStream = new BufferedOutputStream(new FileOutputStream(reportAllTests));
      ExecutionDataWriter dataWriter = new ExecutionDataWriter(outputStream);

      infoStore.accept(dataWriter);
      dataStore.accept(dataWriter);
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to write merged file %s", reportAllTests.getAbsolutePath()), e);
    } finally {
      Closeables.closeQuietly(outputStream);
    }
  }

  private void loadSourceFiles(SessionInfoStore infoStore, ExecutionDataStore dataStore, File... files) {
    for (File file : files) {
      InputStream resourceStream = null;
      try {
        resourceStream = new BufferedInputStream(new FileInputStream(file));
        ExecutionDataReader reader = new ExecutionDataReader(resourceStream);
        reader.setSessionInfoVisitor(infoStore);
        reader.setExecutionDataVisitor(dataStore);
        reader.read();
      } catch (IOException e) {
        throw new SonarException(String.format("Unable to read %s", file.getAbsolutePath()), e);
      } finally {
        Closeables.closeQuietly(resourceStream);
      }
    }
  }

  class AllTestsAnalyzer extends AbstractAnalyzer {
    @Override
    protected String getReportPath(Project project) {
      return MERGED_EXEC;
    }

    @Override
    protected String getExcludes(Project project) {
      return configuration.getExcludes();
    }

    @Override
    protected void saveMeasures(SensorContext context, JavaFile resource, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure mergedMeasure = convertForAllTests(measure);
        if (mergedMeasure != null) {
          context.saveMeasure(resource, mergedMeasure);
        }
      }
    }

    private Measure convertForAllTests(Measure measure) {
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_LINES_TO_COVER, measure.getValue());
      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_UNCOVERED_LINES, measure.getValue());
      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_COVERAGE_LINE_HITS_DATA, measure.getData());
      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_CONDITIONS_TO_COVER, measure.getValue());
      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_UNCOVERED_CONDITIONS, measure.getValue());
      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_COVERED_CONDITIONS_BY_LINE, measure.getData());
      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        return new Measure(CoreMetrics.MERGED_CONDITIONS_BY_LINE, measure.getData());
      }
      return null;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

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
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class JaCoCoOverallSensor implements Sensor {
  public static final String JACOCO_OVERALL = "jacoco-overall.exec";

  private final JacocoConfiguration configuration;

  public JaCoCoOverallSensor(JacocoConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotBlank(configuration.getItReportPath())
      && project.getAnalysisType().isDynamic(true);
  }

  public void analyse(Project project, SensorContext context) {
    ProjectFileSystem fs = project.getFileSystem();

    File reportUTs = fs.resolvePath(configuration.getReportPath());
    File reportITs = fs.resolvePath(configuration.getItReportPath());
    if ((!reportUTs.exists()) || (!reportITs.exists())) {
      return;
    }

    File reportOverall = new File(fs.getSonarWorkingDirectory(), JACOCO_OVERALL);
    reportOverall.getParentFile().mkdirs();

    mergeReports(reportOverall, reportUTs, reportITs);

    new OverallAnalyzer(reportOverall).analyse(project, context);
  }

  private void mergeReports(File reportOverall, File... reports) {
    SessionInfoStore infoStore = new SessionInfoStore();
    ExecutionDataStore dataStore = new ExecutionDataStore();

    loadSourceFiles(infoStore, dataStore, reports);

    BufferedOutputStream outputStream = null;
    try {
      outputStream = new BufferedOutputStream(new FileOutputStream(reportOverall));
      ExecutionDataWriter dataWriter = new ExecutionDataWriter(outputStream);

      infoStore.accept(dataWriter);
      dataStore.accept(dataWriter);
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to write overall coverage report %s", reportOverall.getAbsolutePath()), e);
    } finally {
      Closeables.closeQuietly(outputStream);
    }
  }

  private void loadSourceFiles(SessionInfoStore infoStore, ExecutionDataStore dataStore, File... reports) {
    for (File report : reports) {
      InputStream resourceStream = null;
      try {
        resourceStream = new BufferedInputStream(new FileInputStream(report));
        ExecutionDataReader reader = new ExecutionDataReader(resourceStream);
        reader.setSessionInfoVisitor(infoStore);
        reader.setExecutionDataVisitor(dataStore);
        reader.read();
      } catch (IOException e) {
        throw new SonarException(String.format("Unable to read %s", report.getAbsolutePath()), e);
      } finally {
        Closeables.closeQuietly(resourceStream);
      }
    }
  }

  class OverallAnalyzer extends AbstractAnalyzer {
    private final File report;

    OverallAnalyzer(File report) {
      this.report = report;
    }

    @Override
    protected String getReportPath(Project project) {
      return report.getAbsolutePath();
    }

    @Override
    protected String getExcludes(Project project) {
      return configuration.getExcludes();
    }

    @Override
    protected void saveMeasures(SensorContext context, JavaFile resource, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure mergedMeasure = convertForOverall(measure);
        if (mergedMeasure != null) {
          context.saveMeasure(resource, mergedMeasure);
        }
      }
    }

    private Measure convertForOverall(Measure measure) {
      Measure itMeasure = null;
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_LINES_TO_COVER, measure.getValue());
      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_UNCOVERED_LINES, measure.getValue());
      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, measure.getData());
      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, measure.getValue());
      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, measure.getValue());
      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, measure.getData());
      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, measure.getData());
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

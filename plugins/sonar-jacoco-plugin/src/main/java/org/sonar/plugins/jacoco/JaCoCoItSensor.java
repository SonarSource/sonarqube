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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;

import java.util.Collection;

/**
 * Note that this class can't extend {@link org.sonar.api.batch.AbstractCoverageExtension}, because in this case this extension will be
 * disabled under Sonar 2.3, if JaCoCo is not defined as the default code coverage plugin.
 *
 * @author Evgeny Mandrikov
 */
public class JaCoCoItSensor implements Sensor {
  private JacocoConfiguration configuration;

  public JaCoCoItSensor(JacocoConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotBlank(configuration.getItReportPath())
        && project.getAnalysisType().isDynamic(true);
  }

  public void analyse(Project project, SensorContext context) {
    new ITAnalyzer().analyse(project, context);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    @Override
    protected String getReportPath(Project project) {
      return configuration.getItReportPath();
    }

    @Override
    protected String[] getExcludes(Project project) {
      return configuration.getExcludes();
    }

    @Override
    protected void saveMeasures(SensorContext context, JavaFile resource, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure itMeasure = convertForIT(measure);
        if (itMeasure != null) {
          context.saveMeasure(resource, itMeasure);
        }
      }
    }

    private Measure convertForIT(Measure measure) {
      Measure itMeasure = null;
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_LINES_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_LINES, measure.getValue());

      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, measure.getData());

      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_CONDITIONS, measure.getValue());

      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, measure.getData());

      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_BY_LINE, measure.getData());
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

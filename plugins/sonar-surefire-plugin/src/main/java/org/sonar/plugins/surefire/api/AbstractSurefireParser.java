/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.surefire.api;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.surefire.data.SurefireStaxHandler;
import org.sonar.plugins.surefire.data.UnitTestClassReport;
import org.sonar.plugins.surefire.data.UnitTestIndex;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

/**
 * @since 2.4
 */
public abstract class AbstractSurefireParser {

  public void collect(Project project, SensorContext context, File reportsDir) {
    File[] xmlFiles = getReports(reportsDir);

    if (xmlFiles.length == 0) {
      // See http://jira.codehaus.org/browse/SONAR-2371
      if (project.getModules().isEmpty()) {
        context.saveMeasure(CoreMetrics.TESTS, 0.0);
      }
    } else {
      parseFiles(context, xmlFiles);
    }
  }

  private File[] getReports(File dir) {
    if (dir == null || !dir.isDirectory() || !dir.exists()) {
      return new File[0];
    }
    return dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("TEST") && name.endsWith(".xml");
      }
    });
  }

  private void parseFiles(SensorContext context, File[] reports) {
    UnitTestIndex index = new UnitTestIndex();
    parseFiles(reports, index);
    sanitize(index);
    save(index, context);
  }

  private void parseFiles(File[] reports, UnitTestIndex index) {
    SurefireStaxHandler staxParser = new SurefireStaxHandler(index);
    StaxParser parser = new StaxParser(staxParser, false);
    for (File report : reports) {
      try {
        parser.parse(report);
      } catch (XMLStreamException e) {
        throw new SonarException("Fail to parse the Surefire report: " + report, e);
      }
    }
  }

  private void sanitize(UnitTestIndex index) {
    for (String classname : index.getClassnames()) {
      if (StringUtils.contains(classname, "$")) {
        // Surefire reports classes whereas sonar supports files
        String parentClassName = StringUtils.substringBeforeLast(classname, "$");
        index.merge(classname, parentClassName);
      }
    }
  }

  private void save(UnitTestIndex index, SensorContext context) {
    for (Map.Entry<String, UnitTestClassReport> entry : index.getIndexByClassname().entrySet()) {
      UnitTestClassReport report = entry.getValue();
      if (report.getTests() > 0) {
        Resource resource = getUnitTestResource(entry.getKey());
        double testsCount = report.getTests() - report.getSkipped();
        saveMeasure(context, resource, CoreMetrics.SKIPPED_TESTS, report.getSkipped());
        saveMeasure(context, resource, CoreMetrics.TESTS, testsCount);
        saveMeasure(context, resource, CoreMetrics.TEST_ERRORS, report.getErrors());
        saveMeasure(context, resource, CoreMetrics.TEST_FAILURES, report.getFailures());
        saveMeasure(context, resource, CoreMetrics.TEST_EXECUTION_TIME, report.getDurationMilliseconds());
        double passedTests = testsCount - report.getErrors() - report.getFailures();
        if (testsCount > 0) {
          double percentage = passedTests * 100d / testsCount;
          saveMeasure(context, resource, CoreMetrics.TEST_SUCCESS_DENSITY, ParsingUtils.scaleValue(percentage));
        }
        saveResults(context, resource, report);
      }
    }
  }

  private void saveMeasure(SensorContext context, Resource resource, Metric metric, double value) {
    if (!Double.isNaN(value)) {
      context.saveMeasure(resource, metric, value);
    }
  }

  private void saveResults(SensorContext context, Resource resource, UnitTestClassReport report) {
    context.saveMeasure(resource, new Measure(CoreMetrics.TEST_DATA, report.toXml()));
  }

  protected abstract Resource<?> getUnitTestResource(String classKey);

}

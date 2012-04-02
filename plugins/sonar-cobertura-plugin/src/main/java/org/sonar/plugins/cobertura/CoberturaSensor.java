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
package org.sonar.plugins.cobertura;

import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.Logs;
import org.sonar.plugins.cobertura.api.AbstractCoberturaParser;

import java.io.File;

public class CoberturaSensor implements Sensor, CoverageExtension {

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles();
  }

  public void analyse(Project project, SensorContext context) {
    String path = (String) project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY);
    if (path == null) {
      // wasn't configured - skip
      return;
    }
    File report = project.getFileSystem().resolvePath(path);
    if (!report.exists() || !report.isFile()) {
      LoggerFactory.getLogger(getClass()).warn("Cobertura report not found at {}", report);
      return;
    }
    parseReport(report, context);
  }

  protected void parseReport(File xmlFile, final SensorContext context) {
    LoggerFactory.getLogger(CoberturaSensor.class).info("parsing {}", xmlFile);
    new JavaCoberturaParser().parseReport(xmlFile, context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private static final class JavaCoberturaParser extends AbstractCoberturaParser {
    @Override
    protected Resource<?> getResource(String fileName) {
      return new JavaFile(fileName);
    }
  }
}

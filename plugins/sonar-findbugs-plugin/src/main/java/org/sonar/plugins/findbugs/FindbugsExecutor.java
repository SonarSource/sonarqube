/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @since 2.4
 */
public class FindbugsExecutor implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsExecutor.class);

  private FindbugsConfiguration configuration;

  public FindbugsExecutor(FindbugsConfiguration configuration) {
    this.configuration = configuration;
  }

  public File execute() {
    TimeProfiler profiler = new TimeProfiler().start("Execute Findbugs " + FindbugsVersion.getVersion());
    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());

    OutputStream xmlOutput = null;
    try {
      final FindBugs2 engine = new FindBugs2();

      Project project = configuration.getFindbugsProject();
      engine.setProject(project);

      XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
      xmlBugReporter.setPriorityThreshold(Detector.LOW_PRIORITY);
      xmlBugReporter.setAddMessages(true);
      // xmlBugReporter.setErrorVerbosity(BugReporter.SILENT);

      File xmlReport = configuration.getTargetXMLReport();
      if (xmlReport != null) {
        LOG.info("Findbugs output report: " + xmlReport.getAbsolutePath());
        xmlOutput = FileUtils.openOutputStream(xmlReport);
      } else {
        xmlOutput = new NullOutputStream();
      }
      xmlBugReporter.setOutputStream(new PrintStream(xmlOutput));

      engine.setBugReporter(xmlBugReporter);

      UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
      userPreferences.setEffort(configuration.getEffort());
      engine.setUserPreferences(userPreferences);

      engine.addFilter(configuration.saveIncludeConfigXml().getAbsolutePath(), true);
      engine.addFilter(configuration.saveExcludeConfigXml().getAbsolutePath(), false);

      engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
      engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

      engine.finishSettings();

      Executors.newSingleThreadExecutor().submit(new FindbugsTask(engine)).get(configuration.getTimeout(), TimeUnit.MILLISECONDS);

      profiler.stop();
      return xmlReport;
    } catch (Exception e) {
      throw new SonarException("Can not execute Findbugs", e);
    } finally {
      IOUtils.closeQuietly(xmlOutput);
      Thread.currentThread().setContextClassLoader(initialClassLoader);
    }
  }

  private class FindbugsTask implements Callable<Object> {

    private FindBugs2 engine;

    public FindbugsTask(FindBugs2 engine) {
      this.engine = engine;
    }

    public Object call() throws Exception {
      engine.execute();
      return null;
    }
  }
}

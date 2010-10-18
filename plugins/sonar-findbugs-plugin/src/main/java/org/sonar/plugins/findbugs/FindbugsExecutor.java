package org.sonar.plugins.findbugs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.annotations.Priority;
import edu.umd.cs.findbugs.config.UserPreferences;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @since 2.4
 */
public class FindbugsExecutor implements BatchExtension {
  private static Logger LOG = LoggerFactory.getLogger(FindbugsExecutor.class);

  private FindbugsConfiguration configuration;

  public FindbugsExecutor(FindbugsConfiguration configuration) {
    this.configuration = configuration;
  }

  public File execute() {
    TimeProfiler profiler = new TimeProfiler().start("Execute Findbugs");
    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());

    OutputStream xmlOutput = null;
    try {
      final FindBugs2 engine = new FindBugs2();

      Project project = configuration.getFindbugsProject();
      engine.setProject(project);

      XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
      xmlBugReporter.setPriorityThreshold(Priority.LOW.getPriorityValue());
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

      engine.setProject(project);

      engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
      UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
      userPreferences.setEffort(UserPreferences.EFFORT_DEFAULT);

      engine.addFilter(configuration.saveIncludeConfigXml().getAbsolutePath(), true);
      engine.addFilter(configuration.saveExcludeConfigXml().getAbsolutePath(), false);

      engine.setUserPreferences(userPreferences);
      engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

      engine.finishSettings();

      engine.execute();

      profiler.stop();
      return xmlReport;
    } catch (Exception e) {
      throw new SonarException("Can not execute Findbugs", e);
    } finally {
      IOUtils.closeQuietly(xmlOutput);
      Thread.currentThread().setContextClassLoader(initialClassLoader);
    }
  }

}

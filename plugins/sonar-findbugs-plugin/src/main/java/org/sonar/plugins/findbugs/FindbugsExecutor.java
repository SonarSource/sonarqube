package org.sonar.plugins.findbugs;

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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

      engine.addFilter(configuration.saveIncludeConfigXml(), true);
      engine.addFilter(configuration.saveExcludeConfigXml(), false);

      engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
      engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

      engine.finishSettings();

      Executors.newSingleThreadExecutor()
          .submit(new FindbugsTask(engine))
          .get(configuration.getTimeout(), TimeUnit.MILLISECONDS);

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

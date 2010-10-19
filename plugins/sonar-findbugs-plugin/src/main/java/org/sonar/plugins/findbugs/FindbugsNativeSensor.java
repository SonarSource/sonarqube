package org.sonar.plugins.findbugs;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.io.File;
import java.util.List;

/**
 * EXPERIMENTAL!
 * 
 * @since 2.4
 */
public class FindbugsNativeSensor implements Sensor {

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;

  public FindbugsNativeSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsExecutor executor) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.executor = executor;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles()
        && !profile.getActiveRulesByRepository(FindbugsConstants.REPOSITORY_KEY).isEmpty()
        && project.getPom() != null && !StringUtils.equalsIgnoreCase(project.getPom().getPackaging(), "ear");
  }

  public void analyse(Project project, SensorContext context) {
    File report = getFindbugsReportFile(project);
    if (report == null) {
      report = executor.execute();
    }
    FindbugsXmlReportParser reportParser = new FindbugsXmlReportParser(report);
    List<FindbugsXmlReportParser.Violation> fbViolations = reportParser.getViolations();
    for (FindbugsXmlReportParser.Violation fbViolation : fbViolations) {
      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, fbViolation.getType());
      JavaFile resource = new JavaFile(fbViolation.getSonarJavaFileKey());
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource).setLineId(fbViolation.getStart()).setMessage(fbViolation.getLongMessage());
        context.saveViolation(violation);
      }
    }
  }

  protected final File getFindbugsReportFile(Project project) {
    if (project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH) != null) {
      return new File(project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH));
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}

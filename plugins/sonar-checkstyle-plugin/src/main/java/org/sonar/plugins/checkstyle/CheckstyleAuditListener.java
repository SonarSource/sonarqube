
package org.sonar.plugins.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;

/**
 * @since 2.3
 */
public class CheckstyleAuditListener implements AuditListener, BatchExtension {

  private final SensorContext context;
  private final Project project;
  private final RulesProfile profile;
  private Resource currentResource = null;

  public CheckstyleAuditListener(SensorContext context, Project project, RulesProfile profile) {
    this.context = context;
    this.project = project;
    this.profile = profile;
  }

  public void auditStarted(AuditEvent event) {

  }

  public void auditFinished(AuditEvent event) {

  }

  public void fileStarted(AuditEvent event) {

  }

  public void fileFinished(AuditEvent event) {
    currentResource = null;
  }

  public void addError(AuditEvent event) {
    String ruleKey = getRuleKey(event);
    if (ruleKey != null) {
      ActiveRule activeRule = profile.getActiveRule(CheckstyleConstants.REPOSITORY_KEY, ruleKey);
      if (activeRule != null) {
        initResource(event);
        Violation violation = new Violation(activeRule.getRule(), currentResource)
            .setLineId(getLineId(event))
            .setMessage(getMessage(event));
        context.saveViolation(violation);
      }
    }
  }

  private void initResource(AuditEvent event) {
    if (currentResource == null) {
      String absoluteFilename = event.getFileName();
      currentResource = JavaFile.fromAbsolutePath(absoluteFilename, project.getFileSystem().getSourceDirs(), false);
    }
  }

  private String getRuleKey(AuditEvent event) {
    String key = null;
    try {
      key = event.getModuleId();
    } catch (Exception e) {
      // checkstyle throws a NullPointer if the message is not set
    }
    if (StringUtils.isBlank(key)) {
      try {
        key = event.getSourceName();
      } catch (Exception e) {
        // checkstyle can throw a NullPointer if the message is not set
      }
    }
    return key;
  }

  private String getMessage(AuditEvent event) {
    try {
      return event.getMessage();

    } catch (Exception e) {
      // checkstyle can throw a NullPointer if the message is not set
      return null;
    }
  }

  private int getLineId(AuditEvent event) {
    try {
      return event.getLine();

    } catch (Exception e) {
      // checkstyle can throw a NullPointer if the message is not set
      return 0;
    }
  }

  public void addException(AuditEvent event, Throwable throwable) {
    // TODO waiting for sonar technical events ?
  }

  Resource getCurrentResource() {
    return currentResource;
  }
}

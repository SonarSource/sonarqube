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
package org.sonar.plugins.checkstyle;

import com.google.common.annotations.VisibleForTesting;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

/**
 * @since 2.3
 */
public class CheckstyleAuditListener implements AuditListener, BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleAuditListener.class);

  private final SensorContext context;
  private final Project project;
  private final RuleFinder ruleFinder;
  private Resource currentResource = null;

  public CheckstyleAuditListener(SensorContext context, Project project, RuleFinder ruleFinder) {
    this.context = context;
    this.project = project;
    this.ruleFinder = ruleFinder;
  }

  public void auditStarted(AuditEvent event) {
    // nop
  }

  public void auditFinished(AuditEvent event) {
    // nop
  }

  public void fileStarted(AuditEvent event) {
    // nop
  }

  public void fileFinished(AuditEvent event) {
    currentResource = null;
  }

  public void addError(AuditEvent event) {
    String ruleKey = getRuleKey(event);
    if (ruleKey != null) {
      String message = getMessage(event);
      // In Checkstyle 5.5 exceptions are reported as an events from TreeWalker
      if ("com.puppycrawl.tools.checkstyle.TreeWalker".equals(ruleKey)) {
        LOG.warn(message);
      }
      Rule rule = ruleFinder.findByKey(CheckstyleConstants.REPOSITORY_KEY, ruleKey);
      if (rule != null) {
        initResource(event);
        Violation violation = Violation.create(rule, currentResource)
            .setLineId(getLineId(event))
            .setMessage(message);
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

  @VisibleForTesting
  static String getRuleKey(AuditEvent event) {
    String key = null;
    try {
      key = event.getModuleId();
    } catch (Exception e) {
      // checkstyle throws a NullPointerException if the message is not set
    }
    if (StringUtils.isBlank(key)) {
      try {
        key = event.getSourceName();
      } catch (Exception e) {
        // checkstyle can throw a NullPointerException if the message is not set
      }
    }
    return key;
  }

  @VisibleForTesting
  static String getMessage(AuditEvent event) {
    try {
      return event.getMessage();

    } catch (Exception e) {
      // checkstyle can throw a NullPointerException if the message is not set
      return null;
    }
  }

  @VisibleForTesting
  static Integer getLineId(AuditEvent event) {
    try {
      int line = event.getLine();
      // checkstyle returns 0 if there is no relation to a file content, but we use null
      return line == 0 ? null : line;

    } catch (Exception e) {
      // checkstyle can throw a NullPointerException if the message is not set
      return null;
    }
  }

  /**
   * Note that this method never invoked from Checkstyle 5.5.
   */
  public void addException(AuditEvent event, Throwable throwable) {
    // nop
  }

  Resource getCurrentResource() {
    return currentResource;
  }
}

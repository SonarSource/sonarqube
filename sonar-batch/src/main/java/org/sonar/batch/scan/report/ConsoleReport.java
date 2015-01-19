/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.report;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.Severity;
import org.sonar.batch.issue.IssueCache;

@Properties({
  @Property(key = ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, name = "Enable console report", description = "Set this to true to generate a report in console output",
    type = PropertyType.BOOLEAN, defaultValue = "false")})
public class ConsoleReport implements Reporter {
  private static final Logger LOG = LoggerFactory.getLogger(ConsoleReport.class);

  public static final String CONSOLE_REPORT_ENABLED_KEY = "sonar.issuesReport.console.enable";
  private static final int LEFT_PAD = 10;

  private Settings settings;
  private Logger logger;

  private IssueCache issueCache;

  public ConsoleReport(Settings settings, IssueCache issueCache) {
    this(settings, issueCache, LOG);
  }

  @VisibleForTesting
  public ConsoleReport(Settings settings, IssueCache issueCache, Logger logger) {
    this.settings = settings;
    this.issueCache = issueCache;
    this.logger = logger;
  }

  private static class Report {
    int totalNewIssues = 0;
    int newBlockerIssues = 0;
    int newCriticalIssues = 0;
    int newMajorIssues = 0;
    int newMinorIssues = 0;
    int newInfoIssues = 0;

    public void process(DefaultIssue issue) {
      if (issue.isNew()) {
        totalNewIssues++;
        switch (issue.severity()) {
          case Severity.BLOCKER:
            newBlockerIssues++;
            break;
          case Severity.CRITICAL:
            newCriticalIssues++;
            break;
          case Severity.MAJOR:
            newMajorIssues++;
            break;
          case Severity.MINOR:
            newMinorIssues++;
            break;
          case Severity.INFO:
            newInfoIssues++;
            break;
          default:
            throw new IllegalStateException("Unknow severity: " + issue.severity());
        }
      }
    }
  }

  @Override
  public void execute() {
    if (settings.getBoolean(CONSOLE_REPORT_ENABLED_KEY)) {
      Report r = new Report();
      for (DefaultIssue issue : issueCache.all()) {
        r.process(issue);
      }
      printNewIssues(r);
    }
  }

  public void printNewIssues(Report r) {
    StringBuilder sb = new StringBuilder();

    int newIssues = r.totalNewIssues;
    sb.append("\n\n-------------  Issues Report  -------------\n\n");
    if (newIssues > 0) {
      sb.append(StringUtils.leftPad("+" + newIssues, LEFT_PAD)).append(" issue" + (newIssues > 1 ? "s" : "")).append("\n\n");
      printNewIssues(sb, r.newBlockerIssues, Severity.BLOCKER, "blocking");
      printNewIssues(sb, r.newCriticalIssues, Severity.CRITICAL, "critical");
      printNewIssues(sb, r.newMajorIssues, Severity.MAJOR, "major");
      printNewIssues(sb, r.newMinorIssues, Severity.MINOR, "minor");
      printNewIssues(sb, r.newInfoIssues, Severity.INFO, "info");
    } else {
      sb.append("  No new issue").append("\n");
    }
    sb.append("\n-------------------------------------------\n\n");

    logger.info(sb.toString());
  }

  private void printNewIssues(StringBuilder sb, int issueCount, String severity, String severityLabel) {
    if (issueCount > 0) {
      sb.append(StringUtils.leftPad("+" + issueCount, LEFT_PAD)).append(" ").append(severityLabel).append("\n");
    }
  }
}

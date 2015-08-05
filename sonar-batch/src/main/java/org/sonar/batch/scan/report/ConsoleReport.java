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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.issue.DefaultIssue;

@Properties({
  @Property(key = ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, name = "Enable console report", description = "Set this to true to generate a report in console output",
    type = PropertyType.BOOLEAN, defaultValue = "false")})
public class ConsoleReport implements Reporter {

  @VisibleForTesting
  public static final String HEADER = "-------------  Issues Report  -------------";

  private static final Logger LOG = Loggers.get(ConsoleReport.class);

  public static final String CONSOLE_REPORT_ENABLED_KEY = "sonar.issuesReport.console.enable";
  private static final int LEFT_PAD = 10;

  private Settings settings;
  private IssueCache issueCache;
  private InputPathCache inputPathCache;

  @VisibleForTesting
  public ConsoleReport(Settings settings, IssueCache issueCache, InputPathCache inputPathCache) {
    this.settings = settings;
    this.issueCache = issueCache;
    this.inputPathCache = inputPathCache;
  }

  private static class Report {
    boolean noFile = false;
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

    public void setNoFile(boolean value) {
      this.noFile = value;
    }
  }

  @Override
  public void execute() {
    if (settings.getBoolean(CONSOLE_REPORT_ENABLED_KEY)) {
      Report r = new Report();
      r.setNoFile(!inputPathCache.allFiles().iterator().hasNext());
      for (DefaultIssue issue : issueCache.all()) {
        r.process(issue);
      }
      printReport(r);
    }
  }

  public void printReport(Report r) {
    StringBuilder sb = new StringBuilder();

    sb.append("\n\n" + HEADER + "\n\n");
    if (r.noFile) {
      sb.append("  No file analyzed\n");
    } else {
      printNewIssues(r, sb);
    }
    sb.append("\n-------------------------------------------\n\n");

    LOG.info(sb.toString());
  }

  private void printNewIssues(Report r, StringBuilder sb) {
    int newIssues = r.totalNewIssues;
    if (newIssues > 0) {
      sb.append(StringUtils.leftPad("+" + newIssues, LEFT_PAD)).append(" issue" + (newIssues > 1 ? "s" : "")).append("\n\n");
      printNewIssues(sb, r.newBlockerIssues, Severity.BLOCKER, "blocker");
      printNewIssues(sb, r.newCriticalIssues, Severity.CRITICAL, "critical");
      printNewIssues(sb, r.newMajorIssues, Severity.MAJOR, "major");
      printNewIssues(sb, r.newMinorIssues, Severity.MINOR, "minor");
      printNewIssues(sb, r.newInfoIssues, Severity.INFO, "info");
    } else {
      sb.append("  No new issue").append("\n");
    }
  }

  private static void printNewIssues(StringBuilder sb, int issueCount, String severity, String severityLabel) {
    if (issueCount > 0) {
      sb.append(StringUtils.leftPad("+" + issueCount, LEFT_PAD)).append(" ").append(severityLabel).append("\n");
    }
  }
}

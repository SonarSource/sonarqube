/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.report;

import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleReportTest {

  @Rule
  public LogTester logTester = new LogTester();

  private MapSettings settings;
  private IssueCache issueCache;
  private InputComponentStore inputPathCache;
  private ConsoleReport report;

  @Before
  public void prepare() {
    settings = new MapSettings();
    issueCache = mock(IssueCache.class);
    inputPathCache = mock(InputComponentStore.class);
    report = new ConsoleReport(settings.asConfig(), issueCache, inputPathCache);
  }

  @Test
  public void dontExecuteByDefault() {
    report.execute();
    for (String log : logTester.logs()) {
      assertThat(log).doesNotContain(ConsoleReport.HEADER);
    }
    assertNotDeprecated();
  }

  @Test
  public void testNoFile() {
    settings.setProperty(ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, "true");
    when(inputPathCache.allFilesToPublish()).thenReturn(Collections.emptyList());
    when(issueCache.all()).thenReturn(Collections.emptyList());
    report.execute();
    assertDeprecated();
    assertThat(getReportLog()).isEqualTo(
      "\n\n-------------  Issues Report  -------------\n\n" +
        "  No file analyzed\n" +
        "\n-------------------------------------------\n\n");
  }

  @Test
  public void testNoNewIssue() {
    settings.setProperty(ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, "true");
    when(inputPathCache.allFilesToPublish()).thenReturn(Collections.singleton(new TestInputFileBuilder("foo", "src/Foo.php").build()));
    when(issueCache.all()).thenReturn(Arrays.asList(createIssue(false, null)));
    report.execute();
    assertDeprecated();
    assertThat(getReportLog()).isEqualTo(
      "\n\n-------------  Issues Report  -------------\n\n" +
        "  No new issue\n" +
        "\n-------------------------------------------\n\n");
  }

  @Test
  public void testOneNewIssue() {
    settings.setProperty(ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, "true");
    when(inputPathCache.allFilesToPublish()).thenReturn(Collections.singleton(new TestInputFileBuilder("foo", "src/Foo.php").build()));
    when(issueCache.all()).thenReturn(Arrays.asList(createIssue(true, Severity.BLOCKER)));
    report.execute();
    assertDeprecated();
    assertThat(getReportLog()).isEqualTo(
      "\n\n-------------  Issues Report  -------------\n\n" +
        "        +1 issue\n\n" +
        "        +1 blocker\n" +
        "\n-------------------------------------------\n\n");
  }

  @Test
  public void testOneNewIssuePerSeverity() {
    settings.setProperty(ConsoleReport.CONSOLE_REPORT_ENABLED_KEY, "true");
    when(inputPathCache.allFilesToPublish()).thenReturn(Collections.singleton(new TestInputFileBuilder("foo", "src/Foo.php").build()));
    when(issueCache.all()).thenReturn(Arrays.asList(
      createIssue(true, Severity.BLOCKER),
      createIssue(true, Severity.CRITICAL),
      createIssue(true, Severity.MAJOR),
      createIssue(true, Severity.MINOR),
      createIssue(true, Severity.INFO)));
    report.execute();
    assertDeprecated();
    assertThat(getReportLog()).isEqualTo(
      "\n\n-------------  Issues Report  -------------\n\n" +
        "        +5 issues\n\n" +
        "        +1 blocker\n" +
        "        +1 critical\n" +
        "        +1 major\n" +
        "        +1 minor\n" +
        "        +1 info\n" +
        "\n-------------------------------------------\n\n");
  }

  private void assertDeprecated() {
    assertThat(getLogs()).contains("Console report is deprecated");
  }

  private void assertNotDeprecated() {
    assertThat(getLogs()).doesNotContain("Console report is deprecated");
  }

  private String getLogs() {
    StringBuilder builder = new StringBuilder();
    for (String log : logTester.logs()) {
      builder.append(log).append("\n");
    }
    return builder.toString();
  }

  private String getReportLog() {
    for (String log : logTester.logs()) {
      if (log.contains(ConsoleReport.HEADER)) {
        return log;
      }
    }
    throw new IllegalStateException("No console report");
  }

  private TrackedIssue createIssue(boolean isNew, @Nullable String severity) {
    TrackedIssue issue = new TrackedIssue();
    issue.setNew(isNew);
    issue.setSeverity(severity);

    return issue;
  }

}

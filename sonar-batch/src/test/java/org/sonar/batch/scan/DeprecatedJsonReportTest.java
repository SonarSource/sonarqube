/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.DeprecatedJsonReport;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.java.api.JavaClass;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DeprecatedJsonReportTest {
  DeprecatedJsonReport deprecatedJsonReport;
  DefaultIndex sonarIndex = mock(DefaultIndex.class);
  Resource resource = JavaClass.create("KEY");
  ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
  Server server = mock(Server.class);
  RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
  IssueCache issueCache = mock(IssueCache.class);
  Settings settings;

  @org.junit.Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    settings = new Settings();
    settings.setProperty(CoreProperties.DRY_RUN, true);
    deprecatedJsonReport = spy(new DeprecatedJsonReport(settings, sonarIndex, fileSystem, server, ruleI18nManager, issueCache));
  }

  @Test
  public void should_be_disabled_if_not_dry_run() {
    settings.setProperty(CoreProperties.DRY_RUN, false);
    deprecatedJsonReport.execute();

    verifyZeroInteractions(sonarIndex);
  }

  @Test
  public void should_export_violations() {
    when(server.getVersion()).thenReturn("3.4");
    DefaultIssue issue = new DefaultIssue();
    issue.setComponentKey("KEY");
    issue.setLine(1);
    issue.setMessage("VIOLATION");
    issue.setRuleKey(RuleKey.of("squid", "AvoidCycles"));
    issue.setSeverity(Severity.INFO);
    issue.setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    issue.setCreationDate(DateUtils.parseDate("2013-01-30"));
    issue.setNew(true);
    when(ruleI18nManager.getName("squid", "AvoidCycles", Locale.getDefault())).thenReturn("Avoid Cycles");
    doReturn(Arrays.asList(issue)).when(deprecatedJsonReport).getIssues(resource);

    StringWriter output = new StringWriter();
    deprecatedJsonReport.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();

    assertThat(json)
      .startsWith(
        "{\"version\":\"3.4\",\"violations_per_resource\":{\"KEY\":[{\"line\":1,\"message\":\"VIOLATION\",\"severity\":\"INFO\",\"rule_key\":\"AvoidCycles\",\"rule_repository\":\"squid\",\"rule_name\":\"Avoid Cycles\",\"switched_off\":true,\"is_new\":true,\"created_at\":\"2013-01-30T");
  }

  @Test
  public void should_export_violation_with_no_line() {
    when(server.getVersion()).thenReturn("3.4");
    DefaultIssue issue = new DefaultIssue();
    issue.setComponentKey("KEY");
    issue.setLine(null);
    issue.setMessage("VIOLATION");
    issue.setRuleKey(RuleKey.of("squid", "AvoidCycles"));
    issue.setSeverity(Severity.INFO);
    issue.setResolution(null);
    issue.setNew(false);
    issue.setCreationDate(DateUtils.parseDate("2013-01-30"));
    when(ruleI18nManager.getName("squid", "AvoidCycles", Locale.getDefault())).thenReturn("Avoid Cycles");
    doReturn(Arrays.asList(issue)).when(deprecatedJsonReport).getIssues(resource);

    StringWriter output = new StringWriter();
    deprecatedJsonReport.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();


    assertThat(json).startsWith(
      "{\"version\":\"3.4\",\"violations_per_resource\":{\"KEY\":[{\"message\":\"VIOLATION\",\"severity\":\"INFO\",\"rule_key\":\"AvoidCycles\",\"rule_repository\":\"squid\",\"rule_name\":\"Avoid Cycles\",\"switched_off\":false,\"is_new\":false,\"created_at\":\"2013-01-30");
  }

  @Test
  public void should_ignore_resources_without_violations() {
    when(server.getVersion()).thenReturn("3.4");
    doReturn(Arrays.<DefaultIssue>asList()).when(deprecatedJsonReport).getIssues(resource);

    StringWriter output = new StringWriter();
    deprecatedJsonReport.writeJson(ImmutableSet.of(resource), output);
    String json = output.toString();

    assertThat(json).isEqualTo("{\"version\":\"3.4\",\"violations_per_resource\":{}}");
  }

  @Test
  public void should_export_violations_to_file() throws IOException {
    File sonarDirectory = temporaryFolder.newFolder("sonar");
    when(server.getVersion()).thenReturn("3.4");
    doReturn(Arrays.<DefaultIssue>asList()).when(deprecatedJsonReport).getIssues(resource);
    settings.setProperty("sonar.dryRun.export.path", "output.json");
    when(fileSystem.workingDir()).thenReturn(sonarDirectory);

    deprecatedJsonReport.execute();

    assertThat(new File(sonarDirectory, "output.json")).exists();
  }
}

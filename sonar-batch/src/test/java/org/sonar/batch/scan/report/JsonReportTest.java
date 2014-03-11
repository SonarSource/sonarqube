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

import com.google.common.collect.Lists;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputFileCache;
import org.sonar.core.user.DefaultUser;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonReportTest {

  private SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @org.junit.Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  JsonReport jsonReport;
  Resource resource = mock(Resource.class);
  DefaultFileSystem fs = new DefaultFileSystem();
  Server server = mock(Server.class);
  RuleFinder ruleFinder = mock(RuleFinder.class);
  Settings settings = new Settings();
  IssueCache issueCache = mock(IssueCache.class);
  private AnalysisMode mode;
  private UserFinder userFinder;

  @Before
  public void before() {
    SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
    when(resource.getEffectiveKey()).thenReturn("Action.java");
    when(server.getVersion()).thenReturn("3.6");
    mode = mock(AnalysisMode.class);
    when(mode.isPreview()).thenReturn(true);
    userFinder = mock(UserFinder.class);
    DefaultInputFile inputFile = new DefaultInputFile("src/main/java/org/apache/struts/Action.java");
    inputFile.setKey("struts:src/main/java/org/apache/struts/Action.java");
    inputFile.setStatus(InputFile.Status.CHANGED);
    InputFileCache fileCache = mock(InputFileCache.class);
    when(fileCache.all()).thenReturn(Arrays.<InputFile>asList(inputFile));
    Project rootModule = new Project("struts");
    Project moduleA = new Project("struts-core");
    moduleA.setParent(rootModule).setPath("core");
    Project moduleB = new Project("struts-ui");
    moduleB.setParent(rootModule).setPath("ui");
    jsonReport = new JsonReport(settings, fs, server, ruleFinder, issueCache, mock(EventBus.class),
      mode, userFinder, rootModule, fileCache);
  }

  @Test
  public void should_write_json() throws Exception {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("struts:src/main/java/org/apache/struts/Action.java")
      .setRuleKey(RuleKey.of("squid", "AvoidCycles"))
      .setMessage("There are 2 cycles")
      .setSeverity("MINOR")
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setLine(1)
      .setEffortToFix(3.14)
      .setReporter("julien")
      .setAssignee("simon")
      .setCreationDate(SIMPLE_DATE_FORMAT.parse("2013-04-24"))
      .setUpdateDate(SIMPLE_DATE_FORMAT.parse("2013-04-25"))
      .setNew(false);
    when(ruleFinder.findByKey(RuleKey.of("squid", "AvoidCycles"))).thenReturn(new Rule().setName("Avoid Cycles"));
    when(jsonReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));
    DefaultUser user1 = new DefaultUser().setLogin("julien").setName("Julien");
    DefaultUser user2 = new DefaultUser().setLogin("simon").setName("Simon");
    when(userFinder.findByLogins(eq(Arrays.asList("julien", "simon")))).thenReturn(Lists.<User>newArrayList(user1, user2));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JSONAssert.assertEquals(TestUtils.getResourceContent("/org/sonar/batch/scan/report/JsonReportTest/report.json"),
      writer.toString(), false);
  }

  @Test
  public void should_exclude_resolved_issues() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("struts:src/main/java/org/apache/struts/Action.java")
      .setRuleKey(ruleKey)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setCreationDate(SIMPLE_DATE_FORMAT.parse("2013-04-24"))
      .setUpdateDate(SIMPLE_DATE_FORMAT.parse("2013-04-25"))
      .setCloseDate(SIMPLE_DATE_FORMAT.parse("2013-04-26"))
      .setNew(false);
    when(ruleFinder.findByKey(ruleKey)).thenReturn(Rule.create(ruleKey.repository(), ruleKey.rule()).setName("Avoid Cycles"));
    when(jsonReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JSONAssert.assertEquals(TestUtils.getResourceContent("/org/sonar/batch/scan/report/JsonReportTest/report-without-resolved-issues.json"),
      writer.toString(), false);
  }

  @Test
  public void should_ignore_components_without_issue() throws JSONException {
    when(jsonReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JSONAssert.assertEquals("{\"version\":\"3.6\"}", writer.toString(), false);
  }

  @Test
  public void should_export_issues_to_file() throws IOException {
    File workDir = temporaryFolder.newFolder("sonar");
    fs.setWorkDir(workDir);

    Rule rule = Rule.create("squid", "AvoidCycles").setName("Avoid Cycles");
    when(ruleFinder.findByKey(RuleKey.of("squid", "AvoidCycles"))).thenReturn(rule);
    when(jsonReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    settings.setProperty("sonar.report.export.path", "output.json");

    jsonReport.execute();

    assertThat(new File(workDir, "output.json")).exists();
  }

}

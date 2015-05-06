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
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.batch.repository.user.UserRepository;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.test.JsonAssert;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JSONReportTest {

  private SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JSONReport jsonReport;
  Resource resource = mock(Resource.class);
  DefaultFileSystem fs;
  Server server = mock(Server.class);
  ActiveRules activeRules = mock(ActiveRules.class);
  Settings settings = new Settings();
  IssueCache issueCache = mock(IssueCache.class);
  private UserRepository userRepository;

  @Before
  public void before() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
    when(resource.getEffectiveKey()).thenReturn("Action.java");
    when(server.getVersion()).thenReturn("3.6");
    userRepository = mock(UserRepository.class);
    DefaultInputDir inputDir = new DefaultInputDir("struts", "src/main/java/org/apache/struts");
    DeprecatedDefaultInputFile inputFile = new DeprecatedDefaultInputFile("struts", "src/main/java/org/apache/struts/Action.java");
    inputFile.setStatus(InputFile.Status.CHANGED);
    InputPathCache fileCache = mock(InputPathCache.class);
    when(fileCache.allFiles()).thenReturn(Arrays.<InputFile>asList(inputFile));
    when(fileCache.allDirs()).thenReturn(Arrays.<InputDir>asList(inputDir));
    Project rootModule = new Project("struts");
    Project moduleA = new Project("struts-core");
    moduleA.setParent(rootModule).setPath("core");
    Project moduleB = new Project("struts-ui");
    moduleB.setParent(rootModule).setPath("ui");
    activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("squid", "AvoidCycles")).setName("Avoid Cycles").activate()
      .build();
    jsonReport = new JSONReport(settings, fs, server, activeRules, issueCache, rootModule, fileCache, userRepository);
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
    when(jsonReport.getIssues()).thenReturn(Lists.newArrayList(issue));
    BatchInput.User user1 = BatchInput.User.newBuilder().setLogin("julien").setName("Julien").build();
    BatchInput.User user2 = BatchInput.User.newBuilder().setLogin("simon").setName("Simon").build();
    when(userRepository.loadFromWs(anyListOf(String.class))).thenReturn(Lists.newArrayList(user1, user2));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo(Resources.getResource("org/sonar/batch/scan/report/JsonReportTest/report.json"));
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
    when(jsonReport.getIssues()).thenReturn(Lists.newArrayList(issue));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo(Resources.getResource(
      "org/sonar/batch/scan/report/JsonReportTest/report-without-resolved-issues.json"));
  }

  @Test
  public void should_ignore_components_without_issue() {
    when(jsonReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo("{\"version\":\"3.6\"}");
  }

  @Test
  public void should_export_issues_to_file() throws IOException {
    File workDir = temp.newFolder("sonar");
    fs.setWorkDir(workDir);

    when(jsonReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    settings.setProperty("sonar.report.export.path", "output.json");

    jsonReport.execute();

    assertThat(new File(workDir, "output.json")).exists();
  }

}

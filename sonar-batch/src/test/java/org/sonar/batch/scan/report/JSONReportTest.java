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

import org.sonar.batch.issue.tracking.TrackedIssue;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.test.JsonAssert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JSONReportTest {

  private SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JSONReport jsonReport;
  Resource resource = mock(Resource.class);
  DefaultFileSystem fs;
  Server server = mock(Server.class);
  Rules rules = mock(Rules.class);
  Settings settings = new Settings();
  IssueCache issueCache = mock(IssueCache.class);
  private UserRepositoryLoader userRepository;

  @Before
  public void before() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
    when(resource.getEffectiveKey()).thenReturn("Action.java");
    when(server.getVersion()).thenReturn("3.6");
    userRepository = mock(UserRepositoryLoader.class);
    DefaultInputDir inputDir = new DefaultInputDir("struts", "src/main/java/org/apache/struts");
    DefaultInputFile inputFile = new DefaultInputFile("struts", "src/main/java/org/apache/struts/Action.java");
    inputFile.setStatus(InputFile.Status.CHANGED);
    InputPathCache fileCache = mock(InputPathCache.class);
    when(fileCache.allFiles()).thenReturn(Arrays.<InputFile>asList(inputFile));
    when(fileCache.allDirs()).thenReturn(Arrays.<InputDir>asList(inputDir));
    Project rootModule = new Project("struts");
    Project moduleA = new Project("struts-core");
    moduleA.setParent(rootModule).setPath("core");
    Project moduleB = new Project("struts-ui");
    moduleB.setParent(rootModule).setPath("ui");

    RulesBuilder builder = new RulesBuilder();
    builder.add(RuleKey.of("squid", "AvoidCycles")).setName("Avoid Cycles");
    rules = builder.build();
    jsonReport = new JSONReport(settings, fs, server, rules, issueCache, rootModule, fileCache, userRepository);
  }

  @Test
  public void should_write_json() throws Exception {
    TrackedIssue issue = new TrackedIssue();
    issue.setKey("200");
    issue.setComponentKey("struts:src/main/java/org/apache/struts/Action.java");
    issue.setRuleKey(RuleKey.of("squid", "AvoidCycles"));
    issue.setMessage("There are 2 cycles");
    issue.setSeverity("MINOR");
    issue.setStatus(Issue.STATUS_OPEN);
    issue.setResolution(null);
    issue.setStartLine(1);
    issue.setEndLine(2);
    issue.setStartLineOffset(3);
    issue.setEndLineOffset(4);
    issue.setEffortToFix(3.14);
    issue.setReporter("julien");
    issue.setAssignee("simon");
    issue.setCreationDate(SIMPLE_DATE_FORMAT.parse("2013-04-24"));
    issue.setNew(false);
    when(issueCache.all()).thenReturn(Lists.newArrayList(issue));
    BatchInput.User user1 = BatchInput.User.newBuilder().setLogin("julien").setName("Julien").build();
    BatchInput.User user2 = BatchInput.User.newBuilder().setLogin("simon").setName("Simon").build();
    when(userRepository.load("julien")).thenReturn(user1);
    when(userRepository.load("simon")).thenReturn(user2);

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo(this.getClass().getResource(this.getClass().getSimpleName() + "/report.json"));
  }

  @Test
  public void should_exclude_resolved_issues() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    TrackedIssue issue = new TrackedIssue();
    issue.setKey("200");
    issue.setComponentKey("struts:src/main/java/org/apache/struts/Action.java");
    issue.setRuleKey(ruleKey);
    issue.setStatus(Issue.STATUS_CLOSED);
    issue.setResolution(Issue.RESOLUTION_FIXED);
    issue.setCreationDate(SIMPLE_DATE_FORMAT.parse("2013-04-24"));
    issue.setNew(false);
    when(issueCache.all()).thenReturn(Lists.newArrayList(issue));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo(this.getClass().getResource(this.getClass().getSimpleName() + "/report-without-resolved-issues.json"));
  }

  @Test
  public void should_ignore_components_without_issue() {
    when(issueCache.all()).thenReturn(Collections.<TrackedIssue>emptyList());

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    JsonAssert.assertJson(writer.toString()).isSimilarTo("{\"version\":\"3.6\"}");
  }

  @Test
  public void should_not_export_by_default() throws IOException {
    File workDir = temp.newFolder("sonar");
    fs.setWorkDir(workDir);

    jsonReport.execute();

    verifyZeroInteractions(issueCache);
  }

  @Test
  public void should_export_issues_to_file() throws IOException {
    File workDir = temp.newFolder("sonar");
    fs.setWorkDir(workDir);

    when(issueCache.all()).thenReturn(Collections.<TrackedIssue>emptyList());

    settings.setProperty("sonar.report.export.path", "output.json");

    jsonReport.execute();

    assertThat(new File(workDir, "output.json")).exists();
  }

}

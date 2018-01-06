/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.DefaultComponentTree;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static net.javacrumbs.jsonunit.assertj.JsonAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JSONReportTest {

  private SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private JSONReport jsonReport;
  private DefaultFileSystem fs;
  private Server server = mock(Server.class);
  private Rules rules = mock(Rules.class);
  private MapSettings settings = new MapSettings();
  private IssueCache issueCache = mock(IssueCache.class);
  private InputModuleHierarchy moduleHierarchy;

  @Before
  public void before() throws Exception {
    moduleHierarchy = mock(InputModuleHierarchy.class);
    File projectBaseDir = temp.newFolder();
    fs = new DefaultFileSystem(projectBaseDir.toPath());
    SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
    when(server.getVersion()).thenReturn("3.6");

    DefaultComponentTree inputComponentTree = new DefaultComponentTree();
    ProjectDefinition def = ProjectDefinition.create().setBaseDir(projectBaseDir).setWorkDir(temp.newFolder()).setKey("struts");
    DefaultInputModule rootModule = new DefaultInputModule(def, 1);
    InputComponentStore inputComponentStore = new InputComponentStore(rootModule, mock(BranchConfiguration.class));

    DefaultInputModule moduleA = new DefaultInputModule(ProjectDefinition.create().setKey("struts-core").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    inputComponentTree.index(moduleA, rootModule);
    DefaultInputModule moduleB = new DefaultInputModule(ProjectDefinition.create().setKey("struts-ui").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    inputComponentTree.index(moduleB, rootModule);

    DefaultInputDir inputDir = new DefaultInputDir("struts", "src/main/java/org/apache/struts", TestInputFileBuilder.nextBatchId())
      .setModuleBaseDir(projectBaseDir.toPath());
    DefaultInputFile inputFile = new TestInputFileBuilder("struts", "src/main/java/org/apache/struts/Action.java")
      .setModuleBaseDir(projectBaseDir.toPath()).build();
    inputFile.setStatus(InputFile.Status.CHANGED);
    inputFile.setPublished(true);
    inputComponentStore.put(inputFile);
    inputComponentStore.put(inputDir);

    inputComponentTree.index(inputDir, rootModule);
    inputComponentTree.index(inputFile, inputDir);

    when(moduleHierarchy.children(rootModule)).thenReturn(Arrays.asList(moduleA, moduleB));
    when(moduleHierarchy.parent(moduleA)).thenReturn(rootModule);
    when(moduleHierarchy.parent(moduleB)).thenReturn(rootModule);
    when(moduleHierarchy.relativePath(moduleA)).thenReturn("core");
    when(moduleHierarchy.relativePath(moduleB)).thenReturn("ui");

    RulesBuilder builder = new RulesBuilder();
    builder.add(RuleKey.of("squid", "AvoidCycles")).setName("Avoid Cycles");
    rules = builder.build();
    jsonReport = new JSONReport(moduleHierarchy, settings.asConfig(), fs, server, rules, issueCache, rootModule, inputComponentStore, inputComponentTree);
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
    issue.setGap(3.14);
    issue.setAssignee("simon");
    issue.setCreationDate(SIMPLE_DATE_FORMAT.parse("2013-04-24"));
    issue.setNew(false);
    when(issueCache.all()).thenReturn(Collections.singleton(issue));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    assertThatJson(writer.toString()).isEqualTo(IOUtils.toString(this.getClass().getResource(this.getClass().getSimpleName() + "/report.json")));
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
    when(issueCache.all()).thenReturn(Collections.singleton(issue));

    StringWriter writer = new StringWriter();
    jsonReport.writeJson(writer);

    assertThatJson(writer.toString()).isEqualTo(IOUtils.toString(this.getClass().getResource(this.getClass().getSimpleName() + "/report-without-resolved-issues.json")));
  }

  @Test
  public void should_not_export_by_default() throws IOException {
    File workDir = temp.newFolder("sonar");
    fs.setWorkDir(workDir.toPath());

    jsonReport.execute();

    verifyZeroInteractions(issueCache);
  }

  @Test
  public void should_export_issues_to_file() throws IOException {
    File workDir = temp.newFolder("sonar");
    fs.setWorkDir(workDir.toPath());

    when(issueCache.all()).thenReturn(Collections.<TrackedIssue>emptyList());

    settings.setProperty("sonar.report.export.path", "output.json");

    jsonReport.execute();

    assertThat(new File(workDir, "output.json")).exists();
  }

}

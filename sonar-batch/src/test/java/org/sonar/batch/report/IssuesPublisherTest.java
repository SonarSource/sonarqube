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
package org.sonar.batch.report;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  IssueCache issueCache;
  ProjectDefinition projectDef;
  Project project;

  IssuesPublisher underTest;

  @Before
  public void prepare() {
    projectDef = ProjectDefinition.create().setKey("foo");
    project = new Project("foo").setAnalysisDate(new Date(1234567L));
    BatchComponentCache componentCache = new BatchComponentCache();
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    componentCache.add(project, null);
    componentCache.add(sampleFile, project);
    issueCache = mock(IssueCache.class);
    when(issueCache.byComponent(anyString())).thenReturn(Collections.<DefaultIssue>emptyList());
    underTest = new IssuesPublisher(componentCache, issueCache);
  }

  @Test
  public void write_issues() throws Exception {
    DefaultIssue issue1 = new DefaultIssue();
    issue1.setKey("uuid");
    issue1.setSeverity("MAJOR");
    issue1.setRuleKey(RuleKey.of("repo", "rule"));
    DefaultIssue issue2 = new DefaultIssue();
    issue2.setKey("uuid2");
    issue2.setSeverity("MAJOR");
    issue2.setRuleKey(RuleKey.of("repo", "rule"));
    issue2.setLine(2);
    issue2.setMessage("msg");
    issue2.setEffortToFix(2d);
    issue2.setResolution("FIXED");
    issue2.setStatus("RESOLVED");
    issue2.setChecksum("checksum");
    issue2.setReporter("reporter");
    issue2.setAssignee("assignee");
    issue2.setActionPlanKey("action");
    issue2.setAuthorLogin("author");
    issue2.setCurrentChange(new FieldDiffs().setUserLogin("foo"));
    issue2.setCreationDate(new Date());
    issue2.setUpdateDate(new Date());
    issue2.setCloseDate(new Date());
    issue2.setSelectedAt(1234L);
    when(issueCache.byComponent("foo:src/Foo.php")).thenReturn(Arrays.asList(issue1, issue2));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    underTest.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    assertThat(reader.readComponentIssues(1)).hasSize(0);
    assertThat(reader.readComponentIssues(2)).hasSize(2);
  }

}

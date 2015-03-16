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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.output.BatchReport.Metadata;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private IssueCache issueCache;
  private IssuesPublisher publisher;

  @Before
  public void prepare() {
    ProjectDefinition root = ProjectDefinition.create().setKey("foo");
    Project p = new Project("foo").setAnalysisDate(new Date(1234567L));
    ResourceCache resourceCache = new ResourceCache();
    org.sonar.api.resources.Resource sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(p, null).setSnapshot(new Snapshot().setId(2));
    resourceCache.add(sampleFile, null);
    issueCache = mock(IssueCache.class);
    when(issueCache.byComponent(anyString())).thenReturn(Collections.<DefaultIssue>emptyList());
    publisher = new IssuesPublisher(new ProjectReactor(root), resourceCache, issueCache);
  }

  @Test
  public void publishIssuesAndMetadata() throws Exception {

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
    issue2.setDebt(Duration.create(2));
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

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
    assertThat(metadata.getDeletedComponentsCount()).isEqualTo(0);
    assertThat(metadata.getSnapshotId()).isEqualTo(2);

    assertThat(reader.readComponentIssues(1)).hasSize(0);
    assertThat(reader.readComponentIssues(2)).hasSize(2);

  }

  @Test
  public void publishIssuesOfDeletedComponents() throws Exception {

    DefaultIssue issue1 = new DefaultIssue();
    issue1.setKey("uuid");
    issue1.setComponentUuid("deletedUuid");
    issue1.setSeverity("MAJOR");
    issue1.setRuleKey(RuleKey.of("repo", "rule"));
    DefaultIssue issue2 = new DefaultIssue();
    issue2.setKey("uuid2");
    issue2.setComponentUuid("deletedUuid");
    issue2.setSeverity("MAJOR");
    issue2.setRuleKey(RuleKey.of("repo", "rule"));
    issue2.setLine(2);
    issue2.setMessage("msg");
    issue2.setEffortToFix(2d);
    issue2.setDebt(Duration.create(2));
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

    when(issueCache.byComponent("foo:deleted.php")).thenReturn(Arrays.asList(issue1, issue2));

    when(issueCache.componentKeys()).thenReturn(Arrays.<Object>asList("foo:deleted.php"));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    Metadata metadata = reader.readMetadata();
    assertThat(metadata.getDeletedComponentsCount()).isEqualTo(1);

    assertThat(reader.readComponentIssues(1)).hasSize(0);
    assertThat(reader.readComponentIssues(2)).hasSize(0);
    assertThat(reader.readDeletedComponentIssues(1).getComponentUuid()).isEqualTo("deletedUuid");
    assertThat(reader.readDeletedComponentIssues(1).getIssueList()).hasSize(2);

  }
}

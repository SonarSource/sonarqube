/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.SiblingComponentsWithOpenIssues;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.tracking.SimpleTracker;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class SiblingsIssueMergerIT {
  private final IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  private final Branch branch = mock(Branch.class);

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT, PROJECT_REF).setKey(PROJECT_KEY).setUuid(PROJECT_UUID)
      .addChildren(FILE_1)
      .build());

  @Rule
  public AnalysisMetadataHolderRule metadataHolder = new AnalysisMetadataHolderRule();

  private static final String PROJECT_KEY = "project";
  private static final int PROJECT_REF = 1;
  private static final String PROJECT_UUID = "projectUuid";
  private static final int FILE_1_REF = 12341;
  private static final String FILE_1_KEY = "fileKey";
  private static final String FILE_1_UUID = "fileUuid";

  private static final org.sonar.ce.task.projectanalysis.component.Component FILE_1 = builder(
    org.sonar.ce.task.projectanalysis.component.Component.Type.FILE, FILE_1_REF)
    .setKey(FILE_1_KEY)
    .setUuid(FILE_1_UUID)
    .build();

  private final SimpleTracker<DefaultIssue, SiblingIssue> tracker = new SimpleTracker<>();
  private SiblingsIssueMerger copier;
  private ComponentDto fileOnBranch1Dto;
  private ComponentDto fileOnBranch2Dto;
  private ComponentDto fileOnBranch3Dto;
  private ComponentDto projectDto;
  private ComponentDto branch1Dto;
  private ComponentDto branch2Dto;
  private ComponentDto branch3Dto;
  private RuleDto rule;

  @Before
  public void setUp() {
    DbClient dbClient = db.getDbClient();
    ComponentIssuesLoader componentIssuesLoader = new ComponentIssuesLoader(dbClient, null, null, new MapSettings().asConfig(), System2.INSTANCE,
      mock(IssueChangesToDeleteRepository.class));
    copier = new SiblingsIssueMerger(new SiblingsIssuesLoader(new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, dbClient), dbClient, componentIssuesLoader),
      tracker,
      issueLifecycle);
    projectDto = db.components().insertPublicProject(p -> p.setKey(PROJECT_KEY).setUuid(PROJECT_UUID));
    branch1Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    branch2Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch2")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    branch3Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch3")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    fileOnBranch1Dto = db.components().insertComponent(newFileDto(branch1Dto).setKey(FILE_1_KEY));
    fileOnBranch2Dto = db.components().insertComponent(newFileDto(branch2Dto).setKey(FILE_1_KEY));
    fileOnBranch3Dto = db.components().insertComponent(newFileDto(branch3Dto).setKey(FILE_1_KEY));
    rule = db.rules().insert();
    when(branch.getReferenceBranchUuid()).thenReturn(projectDto.uuid());
    metadataHolder.setBranch(branch);
    metadataHolder.setProject(new Project(projectDto.uuid(), projectDto.getKey(), projectDto.name(), projectDto.description(), Collections.emptyList()));
  }

  @Test
  public void do_nothing_if_no_match() {
    DefaultIssue i = createIssue("issue1", rule.getKey(), Issue.STATUS_CONFIRMED, new Date());
    copier.tryMerge(FILE_1, Collections.singleton(i));

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void do_nothing_if_no_new_issue() {
    db.issues().insert(IssueTesting.newIssue(rule, branch1Dto, fileOnBranch1Dto).setKee("issue1").setStatus(Issue.STATUS_CONFIRMED).setLine(1).setChecksum("checksum"));
    copier.tryMerge(FILE_1, Collections.emptyList());

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void merge_confirmed_issues() {
    db.issues().insert(IssueTesting.newIssue(rule, branch1Dto, fileOnBranch1Dto).setKee("issue1").setStatus(Issue.STATUS_CONFIRMED).setLine(1).setChecksum("checksum"));
    DefaultIssue newIssue = createIssue("issue2", rule.getKey(), Issue.STATUS_OPEN, new Date());

    copier.tryMerge(FILE_1, Collections.singleton(newIssue));

    ArgumentCaptor<DefaultIssue> issueToMerge = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueLifecycle).mergeConfirmedOrResolvedFromPrOrBranch(eq(newIssue), issueToMerge.capture(), eq(BranchType.PULL_REQUEST), eq("myBranch1"));

    assertThat(issueToMerge.getValue().key()).isEqualTo("issue1");
  }

  @Test
  public void prefer_more_recently_updated_issues() {
    Instant now = Instant.now();
    db.issues().insert(IssueTesting.newIssue(rule, branch1Dto, fileOnBranch1Dto).setKee("issue1").setStatus(Issue.STATUS_REOPENED).setLine(1).setChecksum("checksum")
      .setIssueUpdateDate(Date.from(now.plus(2, ChronoUnit.SECONDS))));
    db.issues().insert(IssueTesting.newIssue(rule, branch2Dto, fileOnBranch2Dto).setKee("issue2").setStatus(Issue.STATUS_OPEN).setLine(1).setChecksum("checksum")
      .setIssueUpdateDate(Date.from(now.plus(1, ChronoUnit.SECONDS))));
    db.issues().insert(IssueTesting.newIssue(rule, branch3Dto, fileOnBranch3Dto).setKee("issue3").setStatus(Issue.STATUS_OPEN).setLine(1).setChecksum("checksum")
      .setIssueUpdateDate(Date.from(now)));
    DefaultIssue newIssue = createIssue("newIssue", rule.getKey(), Issue.STATUS_OPEN, new Date());

    copier.tryMerge(FILE_1, Collections.singleton(newIssue));

    ArgumentCaptor<DefaultIssue> issueToMerge = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueLifecycle).mergeConfirmedOrResolvedFromPrOrBranch(eq(newIssue), issueToMerge.capture(), eq(BranchType.PULL_REQUEST), eq("myBranch1"));

    assertThat(issueToMerge.getValue().key()).isEqualTo("issue1");
  }

  @Test
  public void lazy_load_changes() {
    UserDto user = db.users().insertUser();
    IssueDto issue = db.issues()
      .insert(IssueTesting.newIssue(rule, branch2Dto, fileOnBranch2Dto).setKee("issue").setStatus(Issue.STATUS_CONFIRMED).setLine(1).setChecksum("checksum"));
    db.issues().insertComment(issue, user, "A comment 2");
    db.issues().insertFieldDiffs(issue, FieldDiffs.parse("severity=BLOCKER|MINOR,assignee=foo|bar").setCreationDate(new Date()));
    DefaultIssue newIssue = createIssue("newIssue", rule.getKey(), Issue.STATUS_OPEN, new Date());

    copier.tryMerge(FILE_1, Collections.singleton(newIssue));

    ArgumentCaptor<DefaultIssue> issueToMerge = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueLifecycle).mergeConfirmedOrResolvedFromPrOrBranch(eq(newIssue), issueToMerge.capture(), eq(BranchType.PULL_REQUEST), eq("myBranch2"));

    assertThat(issueToMerge.getValue().key()).isEqualTo("issue");
    assertThat(issueToMerge.getValue().defaultIssueComments()).isNotEmpty();
    assertThat(issueToMerge.getValue().changes()).isNotEmpty();
  }

  private static DefaultIssue createIssue(String key, RuleKey ruleKey, String status, Date creationDate) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(key);
    issue.setRuleKey(ruleKey);
    issue.setMessage("msg");
    issue.setLine(1);
    issue.setStatus(status);
    issue.setResolution(null);
    issue.setCreationDate(creationDate);
    issue.setChecksum("checksum");
    return issue;
  }

}

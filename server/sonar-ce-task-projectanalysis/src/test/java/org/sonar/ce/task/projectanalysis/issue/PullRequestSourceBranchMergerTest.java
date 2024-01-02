/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.BlockHashSequence;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class PullRequestSourceBranchMergerTest {
  private static final String PROJECT_KEY = "project";
  private static final String PROJECT_UUID = "projectUuid";
  private static final int FILE_1_REF = 12341;
  private static final String FILE_1_KEY = "fileKey";
  private static final String FILE_1_UUID = "fileUuid";
  private static final org.sonar.ce.task.projectanalysis.component.Component FILE_1 = builder(
    org.sonar.ce.task.projectanalysis.component.Component.Type.FILE, FILE_1_REF)
      .setKey(FILE_1_KEY)
      .setUuid(FILE_1_UUID)
      .build();

  @Mock
  private Tracker<DefaultIssue, DefaultIssue> tracker;

  @Mock
  private IssueLifecycle issueLifecycle;

  @Mock
  private TrackerSourceBranchInputFactory sourceBranchInputFactory;

  @Mock
  private NonClosedTracking<DefaultIssue, DefaultIssue> prTracking;

  @Rule
  public DbTester db = DbTester.create();

  private PullRequestSourceBranchMerger underTest;
  private RuleDto rule;
  private DefaultIssue rawIssue;
  private Input<DefaultIssue> rawIssuesInput;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    underTest = new PullRequestSourceBranchMerger(
      tracker,
      issueLifecycle,
      sourceBranchInputFactory);

    ComponentDto projectDto = db.components().insertPublicProject(p -> p.setKey(PROJECT_KEY).setUuid(PROJECT_UUID));
    ComponentDto branch1Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch1")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    ComponentDto branch2Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch2")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    ComponentDto branch3Dto = db.components().insertProjectBranch(projectDto, b -> b.setKey("myBranch3")
      .setBranchType(BranchType.PULL_REQUEST)
      .setMergeBranchUuid(projectDto.uuid()));
    db.components().insertComponent(newFileDto(branch1Dto).setKey(FILE_1_KEY + ":PULL_REQUEST:myBranch1"));
    db.components().insertComponent(newFileDto(branch2Dto).setKey(FILE_1_KEY + ":PULL_REQUEST:myBranch2"));
    db.components().insertComponent(newFileDto(branch3Dto).setKey(FILE_1_KEY + ":PULL_REQUEST:myBranch3"));
    rule = db.rules().insert();
    rawIssue = createIssue("issue1", rule.getKey(), Issue.STATUS_OPEN, new Date());
    rawIssuesInput = new DefaultTrackingInput(singletonList(rawIssue), mock(LineHashSequence.class), mock(BlockHashSequence.class));
  }

  @Test
  public void tryMergeIssuesFromSourceBranchOfPullRequest_does_nothing_if_source_branch_was_not_analyzed() {
    when(sourceBranchInputFactory.hasSourceBranchAnalysis()).thenReturn(false);

    underTest.tryMergeIssuesFromSourceBranchOfPullRequest(FILE_1, rawIssuesInput.getIssues(), rawIssuesInput);

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void tryMergeIssuesFromSourceBranchOfPullRequest_merges_issue_state_from_source_branch_into_pull_request() {
    DefaultIssue sourceBranchIssue = createIssue("issue2", rule.getKey(), Issue.STATUS_CONFIRMED, new Date());
    Input<DefaultIssue> sourceBranchInput = new DefaultTrackingInput(singletonList(sourceBranchIssue), mock(LineHashSequence.class), mock(BlockHashSequence.class));
    when(sourceBranchInputFactory.hasSourceBranchAnalysis()).thenReturn(true);
    when(sourceBranchInputFactory.createForSourceBranch(any())).thenReturn(sourceBranchInput);
    when(tracker.trackNonClosed(any(), any())).thenReturn(prTracking);
    when(prTracking.getMatchedRaws()).thenReturn(singletonMap(rawIssue, sourceBranchIssue));

    underTest.tryMergeIssuesFromSourceBranchOfPullRequest(FILE_1, rawIssuesInput.getIssues(), rawIssuesInput);

    verify(issueLifecycle).copyExistingIssueFromSourceBranchToPullRequest(rawIssue, sourceBranchIssue);
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

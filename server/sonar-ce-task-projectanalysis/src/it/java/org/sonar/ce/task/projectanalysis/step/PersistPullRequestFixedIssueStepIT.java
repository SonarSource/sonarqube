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
package org.sonar.ce.task.projectanalysis.step;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepositoryImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueFixedDto;

import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class PersistPullRequestFixedIssueStepIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  private PullRequestFixedIssueRepositoryImpl fixedIssueRepository;
  public PersistPullRequestFixedIssueStep underTest;

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  @Before
  public void before() {
    fixedIssueRepository = new PullRequestFixedIssueRepositoryImpl();
    underTest = new PersistPullRequestFixedIssueStep(analysisMetadataHolder, db.getDbClient(), treeRootHolder, fixedIssueRepository);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
  }

  @Test
  public void execute_shouldPersistFixedIssues() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto pullRequest = db.components().insertProjectBranch(projectData.getMainBranchComponent());

    fixedIssueRepository.addFixedIssue(new DefaultIssue().setKey("key1"));
    fixedIssueRepository.addFixedIssue(new DefaultIssue().setKey("key2"));
    treeRootHolder.setRoot(builder(PROJECT, 1)
      .setUuid(pullRequest.uuid())
      .build());

    underTest.execute(new TestComputationStepContext());

    Assertions.assertThat(db.getDbClient().issueFixedDao().selectByPullRequest(db.getSession(), pullRequest.uuid()))
      .extracting(IssueFixedDto::issueKey, IssueFixedDto::pullRequestUuid)
      .containsExactly(tuple("key1", pullRequest.uuid()), tuple("key2", pullRequest.uuid()));
  }

  @Test
  public void execute_whenFixedIssuesAlreadyExists_shouldKeepExistingFixedIssues() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto pullRequest = db.components().insertProjectBranch(projectData.getMainBranchComponent());

    DbSession session = db.getSession();
    db.getDbClient().issueFixedDao().insert(session, new IssueFixedDto(pullRequest.uuid(), "key1"));
    db.getDbClient().issueFixedDao().insert(session, new IssueFixedDto(pullRequest.uuid(), "key2"));
    session.commit();

    fixedIssueRepository.addFixedIssue(new DefaultIssue().setKey("key1"));
    treeRootHolder.setRoot(builder(PROJECT, 1)
      .setUuid(pullRequest.uuid())
      .build());

    underTest.execute(new TestComputationStepContext());

    Assertions.assertThat(db.getDbClient().issueFixedDao().selectByPullRequest(session, pullRequest.uuid()))
      .extracting(IssueFixedDto::issueKey, IssueFixedDto::pullRequestUuid)
      .containsExactly(tuple("key1", pullRequest.uuid()));
  }

  @Test
  public void execute_whenRepositoryIsEmpty_shouldNotPersistAnyFixedIssues() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto pullRequest = db.components().insertProjectBranch(projectData.getMainBranchComponent());

    DbSession session = db.getSession();
    db.getDbClient().issueFixedDao().insert(session, new IssueFixedDto(pullRequest.uuid(), "key1"));
    session.commit();

    treeRootHolder.setRoot(builder(PROJECT, 1)
      .setUuid(pullRequest.uuid())
      .build());

    underTest.execute(new TestComputationStepContext());

    Assertions.assertThat(db.getDbClient().issueFixedDao().selectByPullRequest(db.getSession(), pullRequest.uuid()))
      .isEmpty();
  }


}

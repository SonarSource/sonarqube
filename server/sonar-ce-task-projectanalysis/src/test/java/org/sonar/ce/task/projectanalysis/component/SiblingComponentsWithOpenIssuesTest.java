/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SiblingComponentsWithOpenIssuesTest {
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public AnalysisMetadataHolderRule metadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private SiblingComponentsWithOpenIssues underTest;

  private ComponentDto branch1;
  private ComponentDto fileWithNoIssuesOnBranch1;
  private ComponentDto fileWithOneOpenIssueOnBranch1Pr1;
  private ComponentDto fileWithOneResolvedIssueOnBranch1Pr1;
  private ComponentDto fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1;
  private ComponentDto fileXWithOneResolvedIssueOnBranch1Pr1;
  private ComponentDto fileXWithOneResolvedIssueOnBranch1Pr2;

  private ComponentDto branch2;
  private ComponentDto fileWithOneOpenIssueOnBranch2Pr1;
  private ComponentDto fileWithOneResolvedIssueOnBranch2Pr1;
  private ComponentDto branch1pr1;

  @Before
  public void setUp() {
    ComponentDto project = db.components().insertMainBranch();

    branch1 = db.components().insertProjectBranch(project, b -> b.setKey("branch1"), b -> b.setBranchType(BranchType.BRANCH));
    branch1pr1 = db.components().insertProjectBranch(project,
      b -> b.setKey("branch1pr1"),
      b -> b.setBranchType(BranchType.PULL_REQUEST),
      b -> b.setMergeBranchUuid(branch1.uuid()));
    ComponentDto branch1pr2 = db.components().insertProjectBranch(project,
      b -> b.setKey("branch1pr2"),
      b -> b.setBranchType(BranchType.PULL_REQUEST),
      b -> b.setMergeBranchUuid(branch1.uuid()));

    fileWithNoIssuesOnBranch1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1, null));

    RuleDefinitionDto rule = db.rules().insert();

    fileWithOneOpenIssueOnBranch1Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1pr1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr1, fileWithOneOpenIssueOnBranch1Pr1));

    fileWithOneResolvedIssueOnBranch1Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1pr1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr1, fileWithOneResolvedIssueOnBranch1Pr1).setStatus("RESOLVED"));

    fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1pr1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr1, fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr1, fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1).setStatus("RESOLVED"));

    String fileKey = "file-x";
    fileXWithOneResolvedIssueOnBranch1Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch1pr1, null)
      .setDbKey(fileKey + ":BRANCH:branch1pr1"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr1, fileXWithOneResolvedIssueOnBranch1Pr1).setStatus("RESOLVED"));
    fileXWithOneResolvedIssueOnBranch1Pr2 = db.components().insertComponent(ComponentTesting.newFileDto(branch1pr2, null)
      .setDbKey(fileKey + ":BRANCH:branch1pr2"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch1pr2, fileXWithOneResolvedIssueOnBranch1Pr2).setStatus("RESOLVED"));

    branch2 = db.components().insertProjectBranch(project, b -> b.setKey("branch2"), b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto branch2pr1 = db.components().insertProjectBranch(project,
      b -> b.setKey("branch2pr1"),
      b -> b.setBranchType(BranchType.PULL_REQUEST),
      b -> b.setMergeBranchUuid(branch2.uuid()));

    fileWithOneOpenIssueOnBranch2Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch2pr1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch2pr1, fileWithOneOpenIssueOnBranch2Pr1));

    fileWithOneResolvedIssueOnBranch2Pr1 = db.components().insertComponent(ComponentTesting.newFileDto(branch2pr1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch2pr1, fileWithOneResolvedIssueOnBranch2Pr1).setStatus("RESOLVED"));

    setRoot(branch1);
    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_branch1() {
    setRoot(branch1);
    setBranch(BranchType.BRANCH);

    assertThat(underTest.getUuids(fileWithNoIssuesOnBranch1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnBranch1Pr1.getKey())).containsOnly(fileWithOneOpenIssueOnBranch1Pr1.uuid());
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnBranch1Pr1.getKey())).containsOnly(fileWithOneResolvedIssueOnBranch1Pr1.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1.getKey())).containsOnly(fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1.uuid());

    assertThat(fileXWithOneResolvedIssueOnBranch1Pr1.getKey()).isEqualTo(fileXWithOneResolvedIssueOnBranch1Pr2.getKey());
    assertThat(underTest.getUuids(fileXWithOneResolvedIssueOnBranch1Pr1.getKey())).containsOnly(
      fileXWithOneResolvedIssueOnBranch1Pr1.uuid(),
      fileXWithOneResolvedIssueOnBranch1Pr2.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_pr1() {
    setRoot(branch1pr1);
    setBranch(BranchType.PULL_REQUEST, branch1.uuid());

    assertThat(underTest.getUuids(fileWithNoIssuesOnBranch1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnBranch1Pr1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnBranch1Pr1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenTwoResolvedIssuesOnBranch1Pr1.getKey())).isEmpty();

    assertThat(underTest.getUuids(fileXWithOneResolvedIssueOnBranch1Pr1.getKey())).containsOnly(
      fileXWithOneResolvedIssueOnBranch1Pr2.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_branch2() {
    setRoot(branch2);
    setBranch(BranchType.BRANCH);

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnBranch1Pr1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnBranch2Pr1.getKey())).containsOnly(fileWithOneResolvedIssueOnBranch2Pr1.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnBranch2Pr1.getKey())).containsOnly(fileWithOneOpenIssueOnBranch2Pr1.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_from_pullrequest() {
    ComponentDto project = db.components().insertMainBranch();
    setRoot(project);
    setBranch(BranchType.BRANCH);

    ComponentDto pullRequest = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.PULL_REQUEST),
      b -> b.setMergeBranchUuid(project.uuid()));

    RuleDefinitionDto rule = db.rules().insert();

    ComponentDto fileWithResolvedIssueOnPullrequest = db.components().insertComponent(ComponentTesting.newFileDto(pullRequest, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, pullRequest, fileWithResolvedIssueOnPullrequest).setStatus("RESOLVED"));

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithResolvedIssueOnPullrequest.getKey())).hasSize(1);
  }

  @Test
  public void should_not_find_sibling_components_on_derived_branch() {
    ComponentDto project = db.components().insertMainBranch();
    setRoot(project);
    setBranch(BranchType.BRANCH);

    ComponentDto derivedBranch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.BRANCH),
      b -> b.setMergeBranchUuid(project.uuid()));

    RuleDefinitionDto rule = db.rules().insert();

    ComponentDto fileWithResolvedIssueOnDerivedBranch = db.components().insertComponent(ComponentTesting.newFileDto(derivedBranch, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, derivedBranch, fileWithResolvedIssueOnDerivedBranch).setStatus("RESOLVED"));

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithResolvedIssueOnDerivedBranch.getKey())).isEmpty();
  }

  private void setRoot(ComponentDto componentDto) {
    Component root = mock(Component.class);
    when(root.getUuid()).thenReturn(componentDto.uuid());
    treeRootHolder.setRoot(root);
  }

  private void setBranch(BranchType currentBranchType) {
    setBranch(currentBranchType, null);
  }

  private void setBranch(BranchType currentBranchType, @Nullable String mergeBranchUuid) {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(currentBranchType);
    when(branch.getReferenceBranchUuid()).thenReturn(mergeBranchUuid);
    metadataHolder.setBranch(branch);
  }
}

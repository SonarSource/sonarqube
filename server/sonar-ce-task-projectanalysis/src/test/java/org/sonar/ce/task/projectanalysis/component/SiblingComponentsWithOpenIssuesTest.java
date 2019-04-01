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

  private ComponentDto long1;
  private ComponentDto fileWithNoIssuesOnLong1;
  private ComponentDto fileWithOneOpenIssueOnLong1Short1;
  private ComponentDto fileWithOneResolvedIssueOnLong1Short1;
  private ComponentDto fileWithOneOpenTwoResolvedIssuesOnLong1Short1;
  private ComponentDto fileXWithOneResolvedIssueOnLong1Short1;
  private ComponentDto fileXWithOneResolvedIssueOnLong1Short2;

  private ComponentDto long2;
  private ComponentDto fileWithOneOpenIssueOnLong2Short1;
  private ComponentDto fileWithOneResolvedIssueOnLong2Short1;
  private ComponentDto long1short1;

  @Before
  public void setUp() {
    ComponentDto project = db.components().insertMainBranch();

    long1 = db.components().insertProjectBranch(project, b -> b.setKey("long1"), b -> b.setBranchType(BranchType.LONG));
    long1short1 = db.components().insertProjectBranch(project,
      b -> b.setKey("long1short1"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long1.uuid()));
    ComponentDto long1short2 = db.components().insertProjectBranch(project,
      b -> b.setKey("long1short2"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long1.uuid()));

    fileWithNoIssuesOnLong1 = db.components().insertComponent(ComponentTesting.newFileDto(long1, null));

    RuleDefinitionDto rule = db.rules().insert();

    fileWithOneOpenIssueOnLong1Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenIssueOnLong1Short1));

    fileWithOneResolvedIssueOnLong1Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneResolvedIssueOnLong1Short1).setStatus("RESOLVED"));

    fileWithOneOpenTwoResolvedIssuesOnLong1Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenTwoResolvedIssuesOnLong1Short1));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenTwoResolvedIssuesOnLong1Short1).setStatus("RESOLVED"));

    String fileKey = "file-x";
    fileXWithOneResolvedIssueOnLong1Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null)
      .setDbKey(fileKey + ":BRANCH:long1short1"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileXWithOneResolvedIssueOnLong1Short1).setStatus("RESOLVED"));
    fileXWithOneResolvedIssueOnLong1Short2 = db.components().insertComponent(ComponentTesting.newFileDto(long1short2, null)
      .setDbKey(fileKey + ":BRANCH:long1short2"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short2, fileXWithOneResolvedIssueOnLong1Short2).setStatus("RESOLVED"));

    long2 = db.components().insertProjectBranch(project, b -> b.setKey("long2"), b -> b.setBranchType(BranchType.LONG));
    ComponentDto long2short1 = db.components().insertProjectBranch(project,
      b -> b.setKey("long2short1"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long2.uuid()));

    fileWithOneOpenIssueOnLong2Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long2short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long2short1, fileWithOneOpenIssueOnLong2Short1));

    fileWithOneResolvedIssueOnLong2Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long2short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long2short1, fileWithOneResolvedIssueOnLong2Short1).setStatus("RESOLVED"));

    setRoot(long1);
    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_long1() {
    setRoot(long1);
    setBranch(BranchType.LONG);

    assertThat(underTest.getUuids(fileWithNoIssuesOnLong1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnLong1Short1.getKey())).containsOnly(fileWithOneOpenIssueOnLong1Short1.uuid());
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnLong1Short1.getKey())).containsOnly(fileWithOneResolvedIssueOnLong1Short1.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenTwoResolvedIssuesOnLong1Short1.getKey())).containsOnly(fileWithOneOpenTwoResolvedIssuesOnLong1Short1.uuid());

    assertThat(fileXWithOneResolvedIssueOnLong1Short1.getKey()).isEqualTo(fileXWithOneResolvedIssueOnLong1Short2.getKey());
    assertThat(underTest.getUuids(fileXWithOneResolvedIssueOnLong1Short1.getKey())).containsOnly(
      fileXWithOneResolvedIssueOnLong1Short1.uuid(),
      fileXWithOneResolvedIssueOnLong1Short2.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_short1() {
    setRoot(long1short1);
    setBranch(BranchType.SHORT, long1.uuid());

    assertThat(underTest.getUuids(fileWithNoIssuesOnLong1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnLong1Short1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnLong1Short1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenTwoResolvedIssuesOnLong1Short1.getKey())).isEmpty();

    assertThat(underTest.getUuids(fileXWithOneResolvedIssueOnLong1Short1.getKey())).containsOnly(
      fileXWithOneResolvedIssueOnLong1Short2.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_for_long2() {
    setRoot(long2);
    setBranch(BranchType.LONG);

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnLong1Short1.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnLong2Short1.getKey())).containsOnly(fileWithOneResolvedIssueOnLong2Short1.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnLong2Short1.getKey())).containsOnly(fileWithOneOpenIssueOnLong2Short1.uuid());
  }

  @Test
  public void should_find_sibling_components_with_open_issues_from_short() {
    ComponentDto project = db.components().insertMainBranch();
    setRoot(project);
    setBranch(BranchType.LONG);

    ComponentDto branch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(project.uuid()));

    RuleDefinitionDto rule = db.rules().insert();

    ComponentDto fileWithResolvedIssueOnShort = db.components().insertComponent(ComponentTesting.newFileDto(branch, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, branch, fileWithResolvedIssueOnShort).setStatus("RESOLVED"));

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithResolvedIssueOnShort.getKey())).hasSize(1);
  }

  @Test
  public void should_find_sibling_components_with_open_issues_from_pullrequest() {
    ComponentDto project = db.components().insertMainBranch();
    setRoot(project);
    setBranch(BranchType.LONG);

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
  public void should_not_find_sibling_components_on_derived_long() {
    ComponentDto project = db.components().insertMainBranch();
    setRoot(project);
    setBranch(BranchType.LONG);

    ComponentDto derivedLongBranch = db.components().insertProjectBranch(project,
      b -> b.setBranchType(BranchType.LONG),
      b -> b.setMergeBranchUuid(project.uuid()));

    RuleDefinitionDto rule = db.rules().insert();

    ComponentDto fileWithResolvedIssueOnDerivedLongBranch = db.components().insertComponent(ComponentTesting.newFileDto(derivedLongBranch, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, derivedLongBranch, fileWithResolvedIssueOnDerivedLongBranch).setStatus("RESOLVED"));

    underTest = new SiblingComponentsWithOpenIssues(treeRootHolder, metadataHolder, db.getDbClient());

    assertThat(underTest.getUuids(fileWithResolvedIssueOnDerivedLongBranch.getKey())).isEmpty();
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
    when(branch.getMergeBranchUuid()).thenReturn(mergeBranchUuid);
    metadataHolder.setBranch(branch);
  }
}

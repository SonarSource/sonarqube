package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShortBranchComponentsWithIssuesTest {
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester db = DbTester.create();

  private ShortBranchComponentsWithIssues underTest;
  private Branch branch = mock(Branch.class);

  private ComponentDto long1;
  private ComponentDto fileWithNoIssues;
  private ComponentDto fileWithOneOpenIssue;
  private ComponentDto fileWithOneResolvedIssue;
  private ComponentDto fileWithOneOpenTwoResolvedIssues;
  private ComponentDto fileWithOneResolvedIssueInLong1Short1;
  private ComponentDto fileWithOneResolvedIssueInLong1Short2;

  private ComponentDto long2;
  private ComponentDto fileWithOneOpenIssueOnLong2;
  private ComponentDto fileWithOneResolvedIssueOnLong2;

  @Before
  public void setUp() {
    underTest = new ShortBranchComponentsWithIssues(analysisMetadataHolder, db.getDbClient());
    analysisMetadataHolder.setBranch(branch);
    when(branch.getType()).thenReturn(BranchType.LONG);

    ComponentDto project = db.components().insertMainBranch();

    long1 = db.components().insertProjectBranch(project, b -> b.setKey("long1"), b -> b.setBranchType(BranchType.LONG));
    ComponentDto long1short1 = db.components().insertProjectBranch(project,
      b -> b.setKey("long1short1"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long1.uuid())
    );
    ComponentDto long1short2 = db.components().insertProjectBranch(project,
      b -> b.setKey("long1short2"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long1.uuid())
    );

    fileWithNoIssues = db.components().insertComponent(ComponentTesting.newFileDto(long1, null));

    RuleDefinitionDto rule = db.rules().insert();

    fileWithOneOpenIssue = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenIssue));

    fileWithOneResolvedIssue = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneResolvedIssue).setStatus("RESOLVED"));

    fileWithOneOpenTwoResolvedIssues = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenTwoResolvedIssues));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneOpenTwoResolvedIssues).setStatus("RESOLVED"));

    String fileKey = "file-x";
    fileWithOneResolvedIssueInLong1Short1 = db.components().insertComponent(ComponentTesting.newFileDto(long1short1, null)
      .setDbKey(fileKey + ":BRANCH:long1short1"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short1, fileWithOneResolvedIssueInLong1Short1).setStatus("RESOLVED"));
    fileWithOneResolvedIssueInLong1Short2 = db.components().insertComponent(ComponentTesting.newFileDto(long1short2, null)
      .setDbKey(fileKey + ":BRANCH:long1short2"));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long1short2, fileWithOneResolvedIssueInLong1Short2).setStatus("RESOLVED"));

    long2 = db.components().insertProjectBranch(project, b -> b.setKey("long2"), b -> b.setBranchType(BranchType.LONG));
    ComponentDto long2short1 = db.components().insertProjectBranch(project,
      b -> b.setKey("long2short1"),
      b -> b.setBranchType(BranchType.SHORT),
      b -> b.setMergeBranchUuid(long2.uuid())
    );

    fileWithOneOpenIssueOnLong2 = db.components().insertComponent(ComponentTesting.newFileDto(long2short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long2short1, fileWithOneOpenIssueOnLong2));

    fileWithOneResolvedIssueOnLong2 = db.components().insertComponent(ComponentTesting.newFileDto(long2short1, null));
    db.issues().insertIssue(IssueTesting.newIssue(rule, long2short1, fileWithOneResolvedIssueOnLong2).setStatus("RESOLVED"));
  }

  @Test
  public void should_find_components_with_issues_to_merge_on_long1() {
    analysisMetadataHolder.setUuid(long1.uuid());

    assertThat(underTest.getUuids(fileWithNoIssues.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneOpenIssue.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssue.getKey())).containsOnly(fileWithOneResolvedIssue.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenTwoResolvedIssues.getKey())).containsOnly(fileWithOneOpenTwoResolvedIssues.uuid());

    assertThat(fileWithOneResolvedIssueInLong1Short1.getKey()).isEqualTo(fileWithOneResolvedIssueInLong1Short2.getKey());
    assertThat(underTest.getUuids(fileWithOneResolvedIssueInLong1Short1.getKey())).containsOnly(
      fileWithOneResolvedIssueInLong1Short1.uuid(),
      fileWithOneResolvedIssueInLong1Short2.uuid());
  }

  @Test
  public void should_find_components_with_issues_to_merge_on_long2() {
    analysisMetadataHolder.setUuid(long2.uuid());

    assertThat(underTest.getUuids(fileWithOneResolvedIssue.getKey())).isEmpty();
    assertThat(underTest.getUuids(fileWithOneResolvedIssueOnLong2.getKey())).containsOnly(fileWithOneResolvedIssueOnLong2.uuid());
    assertThat(underTest.getUuids(fileWithOneOpenIssueOnLong2.getKey())).isEmpty();
  }
}

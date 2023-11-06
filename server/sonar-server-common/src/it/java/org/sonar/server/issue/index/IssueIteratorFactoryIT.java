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
package org.sonar.server.issue.index;

import com.google.common.collect.Maps;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.status.IssueStatus;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;

public class IssueIteratorFactoryIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void iterator_over_one_issue() {
    RuleDto rule = dbTester.rules().insert();
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project)
      .setPath("src/main/java/Action.java"));
    IssueDto expected = dbTester.issues().insert(rule, project, file,
      t -> t.setResolution("FIXED")
        .setStatus("RESOLVED")
        .setSeverity("BLOCKER")
        .setManualSeverity(false)
        .setAssigneeUuid("uuid-of-guy1")
        .setAuthorLogin("guy2")
        .setChecksum("FFFFF")
        .setGap(2D)
        .setEffort(10L)
        .setMessage(null)
        .setLine(444)
        .setRuleUuid(rule.getUuid())
        .setTags(List.of("tag1", "tag2", "tag3"))
        .setCreatedAt(1400000000000L)
        .setUpdatedAt(1400000000000L)
        .setIssueCreationDate(new Date(1115848800000L))
        .setIssueUpdateDate(new Date(1356994800000L))
        .setIssueCloseDate(null)
        .setType(2)
        .setCodeVariants(List.of("variant1", "variant2")));

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(1);

    IssueDoc issue = issuesByKey.get(expected.getKey());
    assertThat(issue.key()).isEqualTo(expected.getKey());
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.status()).isEqualTo("RESOLVED");
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FIXED.name());
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.assigneeUuid()).isEqualTo("uuid-of-guy1");
    assertThat(issue.authorLogin()).isEqualTo("guy2");
    assertThat(issue.line()).isEqualTo(444);
    assertThat(issue.ruleUuid()).isEqualTo(rule.getUuid());
    assertThat(issue.componentUuid()).isEqualTo(file.uuid());
    assertThat(issue.projectUuid()).isEqualTo(projectData.projectUuid());
    assertThat(issue.directoryPath()).isEqualTo("src/main/java");
    assertThat(issue.filePath()).isEqualTo("src/main/java/Action.java");
    assertThat(issue.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(issue.effort().toMinutes()).isPositive();
    assertThat(issue.type().getDbConstant()).isEqualTo(2);
    assertThat(issue.getCodeVariants()).containsOnly("variant1", "variant2");
    assertThat(issue.cleanCodeAttributeCategory()).isEqualTo("INTENTIONAL");
  }

  @Test
  public void iterator_over_issues() {
    RuleDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(project, "src/main/java"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(directory, directory)
      .setPath("src/main/java/Action.java"));
    IssueDto fileIssue = dbTester.issues().insert(rule, project, file,
      t -> t
        .setAssigneeUuid("uuid-of-guy1")
        .setAuthorLogin("guy2")
        .setEffort(10L)
        .setType(1));
    IssueDto moduleIssue = dbTester.issues().insert(rule, project, file, t -> t
      .setAssigneeUuid("uuid-of-guy2")
      .setAuthorLogin("guy2")
      .setRuleUuid(rule.getUuid()));
    IssueDto dirIssue = dbTester.issues().insert(rule, project, directory);
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);

    dbTester.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(fileIssue));

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey)
      .hasSize(4)
      .containsOnlyKeys(fileIssue.getKey(), moduleIssue.getKey(), dirIssue.getKey(), projectIssue.getKey());

    assertThat(issuesByKey.get(fileIssue.getKey()).isNewCodeReference()).isTrue();
  }

  @Test
  public void iterator_over_issue_from_project() {
    RuleDto rule = dbTester.rules().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = dbTester.components().insertComponent(newDirectory(project1, "path"));
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project1, dir));
    String[] project1IssueKeys = Stream.of(project1, dir, file1)
      .map(project1Component -> dbTester.issues().insert(rule, project1, project1Component).getKey())
      .toArray(String[]::new);
    ComponentDto project2 = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir2 = dbTester.components().insertComponent(newDirectory(project2, "path"));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project2, dir2));
    String[] project2IssueKeys = Stream.of(project2, dir2, file2)
      .map(project2Component -> dbTester.issues().insert(rule, project2, project2Component).getKey())
      .toArray(String[]::new);

    assertThat(issuesByKey(factory -> factory.createForBranch(project1.uuid())))
      .containsOnlyKeys(project1IssueKeys);
    assertThat(issuesByKey(factory -> factory.createForBranch(project2.uuid())))
      .containsOnlyKeys(project2IssueKeys);
    assertThat(issuesByKey(factory -> factory.createForBranch("does not exist")))
      .isEmpty();
  }

  @Test
  public void extract_directory_path() {
    RuleDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto fileInRootDir = dbTester.components().insertComponent(newFileDto(project).setPath("pom.xml"));
    ComponentDto fileInSubDir = dbTester.components().insertComponent(newFileDto(project).setPath("src/main/java/Action.java"));
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);
    IssueDto fileInSubDirIssue = dbTester.issues().insert(rule, project, fileInSubDir);
    IssueDto fileInRootDirIssue = dbTester.issues().insert(rule, project, fileInRootDir);

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(3);
    assertThat(issuesByKey.get(fileInSubDirIssue.getKey()).directoryPath()).isEqualTo("src/main/java");
    assertThat(issuesByKey.get(fileInRootDirIssue.getKey()).directoryPath()).isEqualTo("/");
    assertThat(issuesByKey.get(projectIssue.getKey()).directoryPath()).isNull();
  }

  @Test
  public void extract_file_path() {
    RuleDto rule = dbTester.rules().insert();
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto fileInRootDir = dbTester.components().insertComponent(newFileDto(project).setPath("pom.xml"));
    ComponentDto fileInSubDir = dbTester.components().insertComponent(newFileDto(project).setPath("src/main/java/Action.java"));
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);
    IssueDto fileInSubDirIssue = dbTester.issues().insert(rule, project, fileInSubDir);
    IssueDto fileInRootDirIssue = dbTester.issues().insert(rule, project, fileInRootDir);

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(3);
    assertThat(issuesByKey.get(fileInSubDirIssue.getKey()).filePath()).isEqualTo("src/main/java/Action.java");
    assertThat(issuesByKey.get(fileInRootDirIssue.getKey()).filePath()).isEqualTo("pom.xml");
    assertThat(issuesByKey.get(projectIssue.getKey()).filePath()).isNull();
  }

  private Map<String, IssueDoc> issuesByKey() {
    return issuesByKey(IssueIteratorFactory::createForAll);
  }

  private Map<String, IssueDoc> issuesByKey(Function<IssueIteratorFactory, IssueIterator> function) {
    try (IssueIterator it = function.apply(new IssueIteratorFactory(dbTester.getDbClient()))) {
      return Maps.uniqueIndex(it, IssueDoc::key);
    }
  }
}

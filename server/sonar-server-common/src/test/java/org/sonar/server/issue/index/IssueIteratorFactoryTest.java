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
package org.sonar.server.issue.index;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class IssueIteratorFactoryTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void iterator_over_one_issue() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
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
        .setRuleId(rule.getId())
        .setIssueAttributes("JIRA=http://jira.com")
        .setTags(ImmutableList.of("tag1", "tag2", "tag3"))
        .setCreatedAt(1400000000000L)
        .setUpdatedAt(1400000000000L)
        .setIssueCreationDate(new Date(1115848800000L))
        .setIssueUpdateDate(new Date(1356994800000L))
        .setIssueCloseDate(null)
        .setType(2));

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(1);

    IssueDoc issue = issuesByKey.get(expected.getKey());
    assertThat(issue.key()).isEqualTo(expected.getKey());
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.status()).isEqualTo("RESOLVED");
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.assigneeUuid()).isEqualTo("uuid-of-guy1");
    assertThat(issue.authorLogin()).isEqualTo("guy2");
    assertThat(issue.line()).isEqualTo(444);
    assertThat(issue.ruleId()).isEqualTo(rule.getId());
    assertThat(issue.componentUuid()).isEqualTo(file.uuid());
    assertThat(issue.projectUuid()).isEqualTo(file.projectUuid());
    assertThat(issue.moduleUuid()).isEqualTo(file.projectUuid());
    assertThat(issue.modulePath()).isEqualTo(file.moduleUuidPath());
    assertThat(issue.directoryPath()).isEqualTo("src/main/java");
    assertThat(issue.filePath()).isEqualTo("src/main/java/Action.java");
    assertThat(issue.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(issue.effort().toMinutes()).isGreaterThan(0L);
    assertThat(issue.type().getDbConstant()).isEqualTo(2);
  }

  @Test
  public void iterator_over_issues() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(project));
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(module, "src/main/java"));
    ComponentDto file = dbTester.components().insertComponent(newFileDto(module, directory)
      .setPath("src/main/java/Action.java"));
    IssueDto fileIssue = dbTester.issues().insert(rule, project, file,
      t -> t
        .setAssigneeUuid("uuid-of-guy1")
        .setAuthorLogin("guy2")
        .setRuleId(rule.getId())
        .setIssueAttributes("JIRA=http://jira.com")
        .setEffort(10L)
        .setType(1));
    IssueDto moduleIssue = dbTester.issues().insert(rule, project, module, t -> t
      .setAssigneeUuid("uuid-of-guy2")
      .setAuthorLogin("guy2")
      .setRuleId(rule.getId())
      .setIssueAttributes("JIRA=http://jira.com"));
    IssueDto dirIssue = dbTester.issues().insert(rule, project, directory);
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(4);
    assertThat(issuesByKey.keySet()).containsOnly(fileIssue.getKey(), moduleIssue.getKey(), dirIssue.getKey(), projectIssue.getKey());
  }

  @Test
  public void iterator_over_issue_from_project() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertPrivateProject(organization);
    ComponentDto module1 = dbTester.components().insertComponent(newModuleDto(project1));
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(module1));
    String[] project1IssueKeys = Stream.of(project1, module1, file1)
      .map(project1Component -> dbTester.issues().insert(rule, project1, project1Component).getKey())
      .toArray(String[]::new);
    ComponentDto project2 = dbTester.components().insertPrivateProject(organization);
    ComponentDto module2 = dbTester.components().insertComponent(newModuleDto(project2));
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(module2));
    String[] project2IssueKeys = Stream.of(project2, module2, file2)
      .map(project2Component -> dbTester.issues().insert(rule, project2, project2Component).getKey())
      .toArray(String[]::new);

    assertThat(issuesByKey(factory -> factory.createForProject(project1.uuid())).keySet())
      .containsOnly(project1IssueKeys);
    assertThat(issuesByKey(factory -> factory.createForProject(project2.uuid())).keySet())
      .containsOnly(project2IssueKeys);
    assertThat(issuesByKey(factory -> factory.createForProject("does not exist")))
      .isEmpty();
  }

  @Test
  public void extract_directory_path() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(project));
    ComponentDto fileInRootDir = dbTester.components().insertComponent(newFileDto(module).setPath("pom.xml"));
    ComponentDto fileInSubDir = dbTester.components().insertComponent(newFileDto(module).setPath("src/main/java/Action.java"));
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);
    IssueDto fileInSubDirIssue = dbTester.issues().insert(rule, project, fileInSubDir);
    IssueDto fileInRootDirIssue = dbTester.issues().insert(rule, project, fileInRootDir);
    IssueDto moduleIssue = dbTester.issues().insert(rule, project, module);

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(4);
    assertThat(issuesByKey.get(fileInSubDirIssue.getKey()).directoryPath()).isEqualTo("src/main/java");
    assertThat(issuesByKey.get(fileInRootDirIssue.getKey()).directoryPath()).isEqualTo("/");
    assertThat(issuesByKey.get(moduleIssue.getKey()).directoryPath()).isNull();
    assertThat(issuesByKey.get(projectIssue.getKey()).directoryPath()).isNull();
  }

  @Test
  public void extract_file_path() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(project));
    ComponentDto fileInRootDir = dbTester.components().insertComponent(newFileDto(module).setPath("pom.xml"));
    ComponentDto fileInSubDir = dbTester.components().insertComponent(newFileDto(module).setPath("src/main/java/Action.java"));
    IssueDto projectIssue = dbTester.issues().insert(rule, project, project);
    IssueDto fileInSubDirIssue = dbTester.issues().insert(rule, project, fileInSubDir);
    IssueDto fileInRootDirIssue = dbTester.issues().insert(rule, project, fileInRootDir);
    IssueDto moduleIssue = dbTester.issues().insert(rule, project, module);

    Map<String, IssueDoc> issuesByKey = issuesByKey();

    assertThat(issuesByKey).hasSize(4);
    assertThat(issuesByKey.get(fileInSubDirIssue.getKey()).filePath()).isEqualTo("src/main/java/Action.java");
    assertThat(issuesByKey.get(fileInRootDirIssue.getKey()).filePath()).isEqualTo("pom.xml");
    assertThat(issuesByKey.get(moduleIssue.getKey()).filePath()).isNull();
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

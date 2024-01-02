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
package org.sonar.ce.task.projectexport.issue;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;

public class ExportIssuesChangelogStepTest {
  private static final String PROJECT_UUID = "project uuid";
  private static final String ISSUE_OPEN_UUID = "issue 1 uuid";
  private static final String ISSUE_CONFIRMED_UUID = "issue 2 uuid";
  private static final String ISSUE_REOPENED_UUID = "issue 3 uuid";
  private static final String ISSUE_RESOLVED_UUID = "issue 4 uuid";
  private static final String ISSUE_CLOSED_UUID = "issue closed uuid";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbClient.openSession(false);
  private final ProjectHolder projectHolder = mock(ProjectHolder.class);
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final ExportIssuesChangelogStep underTest = new ExportIssuesChangelogStep(dbClient, projectHolder, dumpWriter);

  private int issueChangeUuidGenerator = 0;

  @Before
  public void setUp() {
    ComponentDto projectDto = dbTester.components().insertPublicProject(p -> p.setUuid(PROJECT_UUID));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(projectDto));
    when(projectHolder.branches()).thenReturn(newArrayList(
      new BranchDto().setBranchType(BranchType.BRANCH).setKey("master").setProjectUuid(PROJECT_UUID).setUuid(PROJECT_UUID)));

    insertIssue(PROJECT_UUID, ISSUE_OPEN_UUID, STATUS_OPEN);
    insertIssue(PROJECT_UUID, ISSUE_CONFIRMED_UUID, STATUS_CONFIRMED);
    insertIssue(PROJECT_UUID, ISSUE_REOPENED_UUID, STATUS_REOPENED);
    insertIssue(PROJECT_UUID, ISSUE_RESOLVED_UUID, STATUS_RESOLVED);
    insertIssue(PROJECT_UUID, ISSUE_CLOSED_UUID, STATUS_CLOSED);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void getDescription_is_set() {
    assertThat(underTest.getDescription()).isEqualTo("Export issues changelog");
  }

  @Test
  public void execute_writes_now_RuleChange_is_db_is_empty() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG)).isEmpty();
  }

  @Test
  public void execute_writes_entries_of_issues_of_any_type_but_CLOSED() {
    long createdAt = 1_999_993L;
    String[] expectedKeys = new String[] {
      insertIssueChange(ISSUE_OPEN_UUID, createdAt).getKey(),
      insertIssueChange(ISSUE_CONFIRMED_UUID, createdAt + 1).getKey(),
      insertIssueChange(ISSUE_REOPENED_UUID, createdAt + 2).getKey(),
      insertIssueChange(ISSUE_RESOLVED_UUID, createdAt + 3).getKey()
    };
    insertIssueChange(ISSUE_CLOSED_UUID, createdAt + 4);

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG))
      .extracting(ProjectDump.IssueChange::getKey).containsExactly(expectedKeys);
  }

  @Test
  public void execute_writes_only_entries_of_current_project() {
    String issueUuid = "issue uuid";
    insertIssue("other project uuid", issueUuid, STATUS_OPEN);
    insertIssueChange(issueUuid);

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG)).isEmpty();
  }

  @Test
  public void execute_maps_all_fields_to_protobuf() {
    IssueChangeDto issueChangeDto = new IssueChangeDto()
      .setUuid("uuid")
      .setKey("key")
      .setIssueKey(ISSUE_OPEN_UUID)
      .setChangeData("change data")
      .setChangeType("change type")
      .setUserUuid("user_uuid")
      .setIssueChangeCreationDate(454135L)
      .setProjectUuid(PROJECT_UUID);
    insertIssueChange(issueChangeDto);

    underTest.execute(new TestComputationStepContext());

    ProjectDump.IssueChange issueChange = getSingleMessage();
    assertThat(issueChange.getKey()).isEqualTo(issueChangeDto.getKey());
    assertThat(issueChange.getIssueUuid()).isEqualTo(issueChangeDto.getIssueKey());
    assertThat(issueChange.getChangeData()).isEqualTo(issueChangeDto.getChangeData());
    assertThat(issueChange.getChangeType()).isEqualTo(issueChangeDto.getChangeType());
    assertThat(issueChange.getUserUuid()).isEqualTo(issueChangeDto.getUserUuid());
    assertThat(issueChange.getCreatedAt()).isEqualTo(issueChangeDto.getIssueChangeCreationDate());
    assertThat(issueChange.getProjectUuid()).isEqualTo(issueChangeDto.getProjectUuid());
  }

  @Test
  public void execute_exports_issues_by_oldest_create_date_first() {
    long createdAt = 1_999_993L;
    long now = createdAt + 1_000_000_000L;
    String key1 = insertIssueChange(ISSUE_OPEN_UUID, createdAt, now).getKey();
    String key2 = insertIssueChange(ISSUE_OPEN_UUID, createdAt - 500, now + 100).getKey();
    String key3 = insertIssueChange(ISSUE_OPEN_UUID, createdAt - 1000, now + 200).getKey();
    String key4 = insertIssueChange(ISSUE_OPEN_UUID, createdAt + 200, now + 300).getKey();

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG))
      .extracting(ProjectDump.IssueChange::getKey)
      .containsExactly(key3, key2, key1, key4);
  }

  @Test
  public void execute_sets_missing_fields_to_default_values() {
    long createdAt = 1_999_888L;
    insertIssueChange(new IssueChangeDto().setUuid(Uuids.createFast()).setIssueKey(ISSUE_REOPENED_UUID).setCreatedAt(createdAt).setProjectUuid("project_uuid"));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.IssueChange issueChange = getSingleMessage();
    assertThat(issueChange.getKey()).isEmpty();
    assertThat(issueChange.getChangeType()).isEmpty();
    assertThat(issueChange.getChangeData()).isEmpty();
    assertThat(issueChange.getUserUuid()).isEmpty();
    assertThat(issueChange.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  public void execute_sets_createAt_to_zero_if_both_createdAt_and_issueChangeDate_are_null() {
    insertIssueChange(new IssueChangeDto().setUuid(Uuids.createFast()).setIssueKey(ISSUE_REOPENED_UUID).setProjectUuid(PROJECT_UUID));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.IssueChange issueChange = getSingleMessage();
    assertThat(issueChange.getCreatedAt()).isZero();
  }

  @Test
  public void execute_writes_entries_of_closed_issue() {
    insertIssueChange(ISSUE_CLOSED_UUID);

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG)).isEmpty();
  }

  @Test
  public void execute_logs_number_total_exported_issue_changes_count_when_successful() {
    insertIssueChange(ISSUE_OPEN_UUID);
    insertIssueChange(ISSUE_CONFIRMED_UUID);
    insertIssueChange(ISSUE_REOPENED_UUID);

    underTest.execute(new TestComputationStepContext());

    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsExactly("3 issue changes exported");
  }

  @Test
  public void execute_throws_ISE_when_exception_occurs_and_message_contains_number_of_successfully_processed_files_in_() {
    insertIssueChange(ISSUE_OPEN_UUID);
    insertIssueChange(ISSUE_CONFIRMED_UUID);
    insertIssueChange(ISSUE_REOPENED_UUID);
    dumpWriter.failIfMoreThan(2, DumpElement.ISSUES_CHANGELOG);
    TestComputationStepContext context = new TestComputationStepContext();

    assertThatThrownBy(() -> underTest.execute(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Issues changelog export failed after processing 2 issue changes successfully");
  }

  private void insertIssueChange(String issueUuid) {
    insertIssueChange(issueUuid, System2.INSTANCE.now(), null);
  }

  private IssueChangeDto insertIssueChange(String issueUuid, long creationDate) {
    return insertIssueChange(issueUuid, creationDate, null);
  }

  private IssueChangeDto insertIssueChange(String issueUuid, long creationDate, @Nullable Long issueChangeDate) {
    IssueChangeDto dto = new IssueChangeDto()
      .setKey("uuid_" + issueChangeUuidGenerator++)
      .setUuid(Uuids.createFast())
      .setCreatedAt(creationDate)
      .setIssueKey(issueUuid)
      .setProjectUuid("project_uuid");
    if (issueChangeDate != null) {
      dto.setIssueChangeCreationDate(issueChangeDate);
    }
    return insertIssueChange(dto);
  }

  private IssueChangeDto insertIssueChange(IssueChangeDto dto) {
    dbClient.issueChangeDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  private void insertIssue(String projectUuid, String uuid, String status) {
    IssueDto dto = new IssueDto()
      .setKee(uuid)
      .setProjectUuid(projectUuid)
      .setStatus(status);
    dbClient.issueDao().insert(dbSession, dto);
    dbSession.commit();
  }

  private ProjectDump.IssueChange getSingleMessage() {
    List<ProjectDump.IssueChange> messages = dumpWriter.getWrittenMessagesOf(DumpElement.ISSUES_CHANGELOG);
    assertThat(messages).hasSize(1);
    return messages.get(0);
  }
}

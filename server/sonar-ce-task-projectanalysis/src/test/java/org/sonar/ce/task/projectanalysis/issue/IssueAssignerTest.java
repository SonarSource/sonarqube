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

import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.issue.IssueFieldsSetter;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

class IssueAssignerTest {

  private static final int FILE_REF = 1;
  private static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @RegisterExtension
  private final ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();

  @RegisterExtension
  private final AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setAnalysisDate(123456789L);

  private final ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  private final DefaultAssignee defaultAssignee = mock(DefaultAssignee.class);
  private final IssueAssigner underTest = new IssueAssigner(analysisMetadataHolder, scmInfoRepository, scmAccountToUser, defaultAssignee, new IssueFieldsSetter());

  @BeforeEach
  void before() {
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  void do_not_set_author_if_no_changeset() {
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
  }

  @Test
  void set_author_of_new_issue_if_changeset() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
  }

  @Test
  void do_not_reset_author_if_already_set() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1)
      .setAuthorLogin("jane");

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("jane");
  }

  @Test
  void assign_but_do_not_set_author_if_too_long() {
    String scmAuthor = range(0, 256).mapToObj(i -> "s").collect(joining());
    addScmUser(scmAuthor, buildUserId("u123", "John C"));
    setSingleChangeset(scmAuthor, 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.assignee()).isEqualTo("u123");
    assertThat(issue.assigneeLogin()).isEqualTo("John C");

    assertThat(logTester.logs(Level.DEBUG)).contains("SCM account '" + scmAuthor + "' is too long to be stored as issue author");
  }

  @Test
  void assign_new_issue_to_author_of_change() {
    addScmUser("john", buildUserId("u123", "john"));
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
    assertThat(issue.assigneeLogin()).isEqualTo("john");
  }

  @Test
  void assign_new_issue_to_default_assignee_if_author_not_found() {
    setSingleChangeset("john", 123456789L, "rev-1");
    when(defaultAssignee.loadDefaultAssigneeUserId()).thenReturn(new UserIdDto("u1234", "john"));
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u1234");
    assertThat(issue.assigneeLogin()).isEqualTo("john");
  }

  @Test
  void do_not_assign_new_issue_if_no_author_in_changeset() {
    setSingleChangeset(null, 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.assignee()).isNull();
  }

  @Test
  void do_not_assign_issue_if_unassigned_but_already_authored() {
    addScmUser("john", buildUserId("u1234", "john"));
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = newIssueOnLines(1)
      .setAuthorLogin("john")
      .setAssigneeUuid(null);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
    assertThat(issue.assignee()).isNull();
  }

  @Test
  void assign_to_last_committer_of_file_if_issue_is_global_to_file() {
    addScmUser("henry", buildUserId("u123", "Henry V"));
    Changeset changeset1 = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(1_000L)
      .setRevision("rev-1")
      .build();
    // Latest changeset
    Changeset changeset2 = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(2_000L)
      .setRevision("rev-2")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, changeset1, changeset2, changeset1);

    DefaultIssue issue = newIssueOnLines();

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
    assertThat(issue.assigneeLogin()).isEqualTo("Henry V");
  }

  @Test
  void assign_to_default_assignee_if_no_author() {
    DefaultIssue issue = newIssueOnLines();

    when(defaultAssignee.loadDefaultAssigneeUserId()).thenReturn(new UserIdDto("u123", "john"));
    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
    assertThat(issue.assigneeLogin()).isEqualTo("john");
  }

  @Test
  void assign_to_default_assignee_if_no_scm_on_issue_locations() {
    addScmUser("john", buildUserId("u123", "John C"));
    Changeset changeset = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, changeset, changeset);
    DefaultIssue issue = newIssueOnLines(3);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
    assertThat(issue.assigneeLogin()).isEqualTo("John C");
  }

  @Test
  void assign_to_author_of_the_most_recent_change_in_all_issue_locations() {
    addScmUser("john", buildUserId("u1", "John"));
    addScmUser("jane", buildUserId("u2", "Jane"));
    Changeset commit1 = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(1_000L)
      .setRevision("rev-1")
      .build();
    Changeset commit2 = Changeset.newChangesetBuilder()
      .setAuthor("jane")
      .setDate(2_000L)
      .setRevision("rev-2")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, commit1, commit1, commit2, commit1);
    DefaultIssue issue = newIssueOnLines(2, 3, 4);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("jane");
    assertThat(issue.assignee()).isEqualTo("u2");
    assertThat(issue.assigneeLogin()).isEqualTo("Jane");
  }

  private void setSingleChangeset(@Nullable String author, Long date, String revision) {
    scmInfoRepository.setScmInfo(FILE_REF,
      Changeset.newChangesetBuilder()
        .setAuthor(author)
        .setDate(date)
        .setRevision(revision)
        .build());
  }

  private void addScmUser(String scmAccount, UserIdDto userId) {
    when(scmAccountToUser.getNullable(scmAccount)).thenReturn(userId);
  }

  private static DefaultIssue newIssueOnLines(int... lines) {
    DefaultIssue issue = new DefaultIssue();
    issue.setComponentUuid(FILE.getUuid());
    DbIssues.Locations.Builder locations = DbIssues.Locations.newBuilder();
    DbIssues.Flow.Builder flow = DbIssues.Flow.newBuilder();
    Arrays.stream(lines).forEach(line -> flow.addLocation(newLocation(line)));
    locations.addFlow(flow.build());
    issue.setLocations(locations.build());
    return issue;
  }

  private static DbIssues.Location newLocation(int line) {
    return DbIssues.Location.newBuilder()
      .setComponentId(FILE.getUuid())
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(line).setEndLine(line).build()).build();
  }

  private UserIdDto buildUserId(String uuid, String login) {
    return new UserIdDto(uuid, login);
  }
}

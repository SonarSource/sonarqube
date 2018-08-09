/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.IssueFieldsSetter;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class IssueAssignerTest {

  private static final int FILE_REF = 1;
  private static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setAnalysisDate(123456789L);

  private ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  private DefaultAssignee defaultAssignee = mock(DefaultAssignee.class);
  private IssueAssigner underTest = new IssueAssigner(analysisMetadataHolder, scmInfoRepository, scmAccountToUser, defaultAssignee, new IssueFieldsSetter());

  @Test
  public void do_not_set_author_if_no_changeset() {
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
  }

  @Test
  public void set_author_of_new_issue_if_changeset() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
  }

  @Test
  public void do_not_reset_author_if_already_set() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue()
      .setLine(1)
      .setAuthorLogin("jane");

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("jane");
  }

  @Test
  public void assign_but_do_not_set_author_if_too_long() {
    String scmAuthor = range(0, 256).mapToObj(i -> "s").collect(joining());
    addScmUser(scmAuthor, "John C");
    setSingleChangeset(scmAuthor, 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.assignee()).isEqualTo("John C");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("SCM account '" + scmAuthor + "' is too long to be stored as issue author");
  }

  @Test
  public void assign_new_issue_to_author_of_change() {
    addScmUser("john", "u123");
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
  }

  @Test
  public void assign_new_issue_to_default_assignee_if_author_not_found() {
    setSingleChangeset("john", 123456789L, "rev-1");
    when(defaultAssignee.loadDefaultAssigneeUuid()).thenReturn("u1234");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u1234");
  }

  @Test
  public void do_not_assign_new_issue_if_no_author_in_changeset() {
    setSingleChangeset(null, 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void do_not_assign_issue_if_unassigned_but_already_authored() {
    addScmUser("john", "u1234");
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1)
      .setAuthorLogin("john")
      .setAssigneeUuid(null);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void assign_to_last_committer_of_file_if_issue_is_global_to_file() {
    addScmUser("henry", "Henry V");
    Changeset changeset1 = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();
    // Latest changeset
    Changeset changeset2 = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(1234567810L)
      .setRevision("rev-2")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, changeset1, changeset2, changeset1);

    DefaultIssue issue = new DefaultIssue().setLine(null);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("Henry V");
  }

  @Test
  public void assign_to_default_assignee_if_no_author() {
    DefaultIssue issue = new DefaultIssue().setLine(null);

    when(defaultAssignee.loadDefaultAssigneeUuid()).thenReturn("u123");
    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("u123");
  }

  @Test
  public void assign_to_default_assignee_if_scm_on_issue_locations() {
    addScmUser("john", "John C");
    Changeset changeset = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, changeset, changeset);
    DefaultIssue issue = new DefaultIssue().setLine(3);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  @Test
  public void display_warning_when_line_is_above_max_size() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(2).setType(RuleType.VULNERABILITY);

    underTest.onIssue(FILE, issue);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "No SCM info has been found for issue DefaultIssue[key=<null>,type=VULNERABILITY,componentUuid=<null>,componentKey=<null>," +
        "moduleUuid=<null>,moduleUuidPath=<null>,projectUuid=<null>,projectKey=<null>,ruleKey=<null>,language=<null>,severity=<null>," +
        "manualSeverity=false,message=<null>,line=2,gap=<null>,effort=<null>,status=<null>,resolution=<null>," +
        "assigneeUuid=<null>,checksum=<null>,attributes=<null>,authorLogin=<null>,comments=<null>,tags=<null>," +
        "locations=<null>,isFromExternalRuleEngine=false,creationDate=<null>,updateDate=<null>,closeDate=<null>,isFromHotspot=false,currentChange=<null>,changes=<null>,isNew=true,isCopied=false," +
        "beingClosed=false,onDisabledRule=false,isChanged=false,sendNotifications=false,selectedAt=<null>]");
  }

  private void setSingleChangeset(String author, Long date, String revision) {
    scmInfoRepository.setScmInfo(FILE_REF,
      Changeset.newChangesetBuilder()
        .setAuthor(author)
        .setDate(date)
        .setRevision(revision)
        .build());
  }

  private void addScmUser(String scmAccount, String userUuid) {
    when(scmAccountToUser.getNullable(scmAccount)).thenReturn(userUuid);
  }

}

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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepositoryRule;
import org.sonar.server.issue.IssueFieldsSetter;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class IssueAssignerTest {

  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setAnalysisDate(123456789L);

  ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  DefaultAssignee defaultAssignee = mock(DefaultAssignee.class);

  IssueAssigner underTest = new IssueAssigner(analysisMetadataHolder, scmInfoRepository, scmAccountToUser, defaultAssignee, new IssueFieldsSetter());

  @Test
  public void nothing_to_do_if_no_changeset() {
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
  }

  @Test
  public void set_author_to_issue() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
  }

  @Test
  public void does_not_set_author_to_issue_if_already_set() {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue()
      .setLine(1)
      .setAuthorLogin("j1234");

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("j1234");
  }

  @Test
  public void set_assignee_to_issue() {
    addScmUser("john", "John C");
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  @Test
  public void dont_store_author_too_long() {
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
  public void set_default_assignee_if_author_not_found() {
    addScmUser("john", null);
    setSingleChangeset("john", 123456789L, "rev-1");
    when(defaultAssignee.loadDefaultAssigneeLogin()).thenReturn("John C");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  @Test
  public void doest_not_set_assignee_if_no_author() {
    addScmUser("john", "John C");
    setSingleChangeset(null, 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void doest_not_set_assignee_if_author_already_set_and_assignee_null() {
    addScmUser("john", "John C");
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setLine(1)
      .setAuthorLogin("john")
      .setAssignee(null);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void set_last_committer_when_line_is_null() {
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
  public void when_noscm_data_is_available_defaultAssignee_should_be_used() {
    DefaultIssue issue = new DefaultIssue().setLine(null);

    when(defaultAssignee.loadDefaultAssigneeLogin()).thenReturn("DefaultAssignee");
    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("DefaultAssignee");
  }

  @Test
  public void set_last_committer_when_line_is_bigger_than_changeset_size() {
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
        "assignee=<null>,checksum=<null>,attributes=<null>,authorLogin=<null>,comments=<null>,tags=<null>," +
        "locations=<null>,creationDate=<null>,updateDate=<null>,closeDate=<null>,currentChange=<null>,changes=<null>,isNew=true,isCopied=false," +
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

  private void addScmUser(String scmAccount, String userName) {
    when(scmAccountToUser.getNullable(scmAccount)).thenReturn(userName);
  }

}

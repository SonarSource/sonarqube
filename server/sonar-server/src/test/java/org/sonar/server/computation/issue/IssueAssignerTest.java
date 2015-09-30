/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.issue;

import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.scm.Changeset;
import org.sonar.server.computation.scm.ScmInfoRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class IssueAssignerTest {

  static final int FILE_REF = 1;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF).setKey("FILE_KEY").setUuid("FILE_UUID").build();

  @org.junit.Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();

  ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  DefaultAssignee defaultAssignee = mock(DefaultAssignee.class);

  IssueAssigner underTest = new IssueAssigner(scmInfoRepository, scmAccountToUser, defaultAssignee);

  @Test
  public void set_author_to_issue() throws Exception {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setNew(true).setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isEqualTo("john");
  }

  @Test
  public void nothing_to_do_if_issue_is_not_new() throws Exception {
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setNew(false).setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
  }

  @Test
  public void nothing_to_do_if_no_changeset() throws Exception {
    DefaultIssue issue = new DefaultIssue().setNew(true).setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.authorLogin()).isNull();
  }

  @Test
  public void set_assignee_to_issue() throws Exception {
    addScmUser("john", "John C");
    setSingleChangeset("john", 123456789L, "rev-1");
    DefaultIssue issue = new DefaultIssue().setNew(true).setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  @Test
  public void set_default_assignee_if_author_not_found() throws Exception {
    addScmUser("john", null);
    setSingleChangeset("john", 123456789L, "rev-1");
    when(defaultAssignee.getLogin()).thenReturn("John C");
    DefaultIssue issue = new DefaultIssue().setNew(true).setLine(1);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  @Test
  public void set_last_committer_when_line_is_null() throws Exception {
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

    DefaultIssue issue = new DefaultIssue()
      .setNew(true)
      .setLine(null);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("Henry V");
  }

  @Test
  public void set_last_committer_when_line_is_bigger_than_changeset_size() throws Exception {
    addScmUser("john", "John C");
    Changeset changeset = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();
    scmInfoRepository.setScmInfo(FILE_REF, changeset, changeset);
    DefaultIssue issue = new DefaultIssue().setNew(true).setLine(3);

    underTest.onIssue(FILE, issue);

    assertThat(issue.assignee()).isEqualTo("John C");
  }

  private void setSingleChangeset(String author, Long date, String revision) {
    scmInfoRepository.setScmInfo(FILE_REF,
      Changeset.newChangesetBuilder()
        .setAuthor(author)
        .setDate(date)
        .setRevision(revision)
        .build());
  }

  private void addScmUser(String scmAccount, String userName){
    when(scmAccountToUser.getNullable(scmAccount)).thenReturn(userName);
  }

}

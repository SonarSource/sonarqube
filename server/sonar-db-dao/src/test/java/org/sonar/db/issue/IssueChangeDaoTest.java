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
package org.sonar.db.issue;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;
import static org.sonar.db.issue.IssueChangeDto.TYPE_FIELD_CHANGE;

public class IssueChangeDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private IssueChangeDao underTest = db.getDbClient().issueChangeDao();

  @Test
  public void select_issue_changelog_from_issue_key() {
    IssueDto issue1 = db.issues().insertIssue();
    db.issues().insertChange(issue1, c -> c.setChangeType(TYPE_FIELD_CHANGE).setChangeData("severity=MAJOR|BLOCKER"));
    IssueDto issue2 = db.issues().insertIssue();
    db.issues().insertChange(issue2);

    List<FieldDiffs> changelog = underTest.selectChangelogByIssue(db.getSession(), issue1.getKey());

    assertThat(changelog).hasSize(1);
    assertThat(changelog.get(0).diffs()).hasSize(1);
    assertThat(changelog.get(0).diffs().get("severity").newValue()).isEqualTo("BLOCKER");
    assertThat(changelog.get(0).diffs().get("severity").oldValue()).isEqualTo("MAJOR");

    assertThat(underTest.selectChangelogByIssue(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void select_issue_changes_from_issues_keys() {
    IssueDto issue1 = db.issues().insertIssue();
    db.issues().insertChange(issue1);
    db.issues().insertChange(issue1);
    db.issues().insertChange(issue1);
    db.issues().insertChange(issue1);
    IssueDto issue2 = db.issues().insertIssue();
    db.issues().insertChange(issue2);
    IssueDto issue3 = db.issues().insertIssue();

    List<IssueChangeDto> changelog = underTest.selectByIssueKeys(db.getSession(), asList(issue1.getKey(), issue2.getKey(), issue3.getKey()));

    assertThat(changelog).hasSize(5);

    assertThat(underTest.selectByIssueKeys(db.getSession(), singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectByIssueKeys(db.getSession(), emptyList())).isEmpty();
  }

  @Test
  public void select_comment_by_key() {
    IssueDto issue = db.issues().insertIssue();
    IssueChangeDto issueChange = db.issues().insertChange(issue, c -> c.setChangeType(TYPE_COMMENT));
    db.issues().insertChange(issue, c -> c.setChangeType(TYPE_COMMENT));

    Optional<IssueChangeDto> issueChangeDto = underTest.selectCommentByKey(db.getSession(), issueChange.getKey());

    assertThat(issueChangeDto).isPresent();
    assertThat(issueChangeDto.get().getKey()).isEqualTo(issueChange.getKey());
    assertThat(issueChangeDto.get().getChangeType()).isEqualTo(TYPE_COMMENT);
    assertThat(issueChangeDto.get().getUserUuid()).isEqualTo(issueChange.getUserUuid());
    assertThat(issueChangeDto.get().getChangeData()).isEqualTo(issueChange.getChangeData());
    assertThat(issueChangeDto.get().getIssueChangeCreationDate()).isNotNull();
    assertThat(issueChangeDto.get().getCreatedAt()).isNotNull();
    assertThat(issueChangeDto.get().getUpdatedAt()).isNotNull();
  }

  @Test
  public void delete() {
    IssueDto issue = db.issues().insertIssue();
    IssueChangeDto issueChange1 = db.issues().insertChange(issue);
    IssueChangeDto issueChange2 = db.issues().insertChange(issue);

    assertThat(underTest.delete(db.getSession(), issueChange1.getKey())).isTrue();

    assertThat(db.countRowsOfTable(db.getSession(), "issue_changes")).isEqualTo(1);
  }

  @Test
  public void delete_unknown_key() {
    IssueDto issue = db.issues().insertIssue();
    db.issues().insertChange(issue);

    assertThat(underTest.delete(db.getSession(), "UNKNOWN")).isFalse();
  }

  @Test
  public void insert() {
    IssueDto issue = db.issues().insertIssue();
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserUuid("user_uuid")
      .setChangeData("Some text")
      .setChangeType("comment")
      .setIssueKey(issue.getKey())
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_501_000_000_000L)
      .setIssueChangeCreationDate(1_502_000_000_000L);

    underTest.insert(db.getSession(), changeDto);
    db.getSession().commit();

    assertThat(underTest.selectByIssueKeys(db.getSession(), singletonList(issue.getKey())))
      .extracting(IssueChangeDto::getKey, IssueChangeDto::getIssueKey, IssueChangeDto::getChangeData, IssueChangeDto::getChangeType,
        IssueChangeDto::getIssueChangeCreationDate, IssueChangeDto::getCreatedAt, IssueChangeDto::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple("EFGH", issue.getKey(), "Some text", TYPE_COMMENT, 1_502_000_000_000L, 1_500_000_000_000L, 1_501_000_000_000L));
  }

  @Test
  public void update() {
    IssueDto issue = db.issues().insertIssue();
    IssueChangeDto issueChange = db.issues().insertChange(issue);

    assertThat(underTest.update(db.getSession(), new IssueChangeDto()
      .setKey(issueChange.getKey())
      // Only the following fields can be updated:
      .setChangeData("new comment")
      .setUpdatedAt(1_500_000_000_000L)
      // Should not be taking into account
      .setIssueKey("other_issue_uuid")
      .setUserUuid("other_user_uuid")
      .setCreatedAt(10_000_000_000L)
      .setIssueChangeCreationDate(30_000_000_000L))).isTrue();
    db.commit();

    assertThat(underTest.selectByIssueKeys(db.getSession(), singletonList(issue.getKey())))
      .extracting(IssueChangeDto::getKey, IssueChangeDto::getIssueKey, IssueChangeDto::getChangeData, IssueChangeDto::getChangeType,
        IssueChangeDto::getIssueChangeCreationDate, IssueChangeDto::getCreatedAt, IssueChangeDto::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple(issueChange.getKey(), issue.getKey(), "new comment", issueChange.getChangeType(), issueChange.getIssueChangeCreationDate(), issueChange.getCreatedAt(),
          1_500_000_000_000L));
  }

  @Test
  public void update_unknown_key() {
    IssueDto issue = db.issues().insertIssue();
    IssueChangeDto issueChange = db.issues().insertChange(issue);

    assertThat(underTest.update(db.getSession(), new IssueChangeDto()
      .setKey("UNKNOWN")
      .setIssueKey("other_issue_uuid")
      .setChangeData("new comment")
      .setUpdatedAt(DateUtils.parseDate("2013-06-30").getTime())))
        .isFalse();

    assertThat(underTest.selectByIssueKeys(db.getSession(), singletonList(issue.getKey())))
      .extracting(IssueChangeDto::getKey, IssueChangeDto::getIssueKey, IssueChangeDto::getChangeData, IssueChangeDto::getChangeType,
        IssueChangeDto::getIssueChangeCreationDate, IssueChangeDto::getCreatedAt, IssueChangeDto::getUpdatedAt)
      .containsExactlyInAnyOrder(
        tuple(issueChange.getKey(), issue.getKey(), issueChange.getChangeData(), issueChange.getChangeType(), issueChange.getIssueChangeCreationDate(), issueChange.getCreatedAt(),
          issueChange.getUpdatedAt()));
  }

}

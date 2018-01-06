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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueChangeDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private IssueChangeDao underTest = db.getDbClient().issueChangeDao();

  @Test
  public void select_issue_changelog_from_issue_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<FieldDiffs> changelog = underTest.selectChangelogByIssue(db.getSession(), "1000");
    assertThat(changelog).hasSize(1);
    assertThat(changelog.get(0).diffs()).hasSize(1);
    assertThat(changelog.get(0).diffs().get("severity").newValue()).isEqualTo("BLOCKER");
    assertThat(changelog.get(0).diffs().get("severity").oldValue()).isEqualTo("MAJOR");
  }

  @Test
  public void select_issue_changes_from_issues_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<IssueChangeDto> changelog = underTest.selectByIssueKeys(db.getSession(), Arrays.asList("1000", "1001"));
    assertThat(changelog).hasSize(5);
  }

  @Test
  public void selectChangelogOfNonClosedIssuesByComponent() {
    db.prepareDbUnit(getClass(), "selectChangelogOfNonClosedIssuesByComponent.xml");

    List<IssueChangeDto> dtos = underTest.selectChangelogOfNonClosedIssuesByComponent(db.getSession(), "FILE_1");
    // no need to have ordered results (see NewDebtCalculator)
    assertThat(dtos).extracting("id").containsOnly(100L, 103L);
  }

  @Test
  public void select_comment_by_key() {
    IssueDto issueDto = db.issues().insertIssue();
    IssueChangeDto comment = db.issues().insertComment(issueDto, "john", "some comment");

    Optional<IssueChangeDto> issueChangeDto = underTest.selectCommentByKey(db.getSession(), comment.getKey());

    assertThat(issueChangeDto).isPresent();
    assertThat(issueChangeDto.get().getKey()).isEqualTo(comment.getKey());
    assertThat(issueChangeDto.get().getChangeType()).isEqualTo(IssueChangeDto.TYPE_COMMENT);
    assertThat(issueChangeDto.get().getUserLogin()).isEqualTo("john");
    assertThat(issueChangeDto.get().getChangeData()).isEqualTo("some comment");
    assertThat(issueChangeDto.get().getIssueChangeCreationDate()).isNotNull();
    assertThat(issueChangeDto.get().getCreatedAt()).isNotNull();
    assertThat(issueChangeDto.get().getUpdatedAt()).isNotNull();
  }

  @Test
  public void delete() {
    db.prepareDbUnit(getClass(), "delete.xml");

    assertThat(underTest.delete(db.getSession(), "COMMENT-2")).isTrue();
    db.commit();

    db.assertDbUnit(getClass(), "delete-result.xml", "issue_changes");
  }

  @Test
  public void delete_unknown_key() {
    db.prepareDbUnit(getClass(), "delete.xml");

    assertThat(underTest.delete(db.getSession(), "UNKNOWN")).isFalse();
  }

  @Test
  public void insert() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserLogin("emmerik")
      .setChangeData("Some text")
      .setChangeType("comment")
      .setIssueKey("ABCDE")
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(1_501_000_000_000L)
      .setIssueChangeCreationDate(1_502_000_000_000L);

    underTest.insert(db.getSession(), changeDto);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "issue_changes");
  }

  @Test
  public void update() {
    db.prepareDbUnit(getClass(), "update.xml");

    IssueChangeDto change = new IssueChangeDto();
    change.setKey("COMMENT-2");

    // Only the following fields can be updated:
    change.setChangeData("new comment");
    change.setUpdatedAt(1_500_000_000_000L);

    assertThat(underTest.update(db.getSession(), change)).isTrue();
    db.commit();

    db.assertDbUnit(getClass(), "update-result.xml", "issue_changes");
  }

  @Test
  public void update_unknown_key() {
    db.prepareDbUnit(getClass(), "update.xml");

    IssueChangeDto change = new IssueChangeDto();
    change.setKey("UNKNOWN");
    change.setChangeData("new comment");
    change.setUpdatedAt(DateUtils.parseDate("2013-06-30").getTime());

    assertThat(underTest.update(db.getSession(), change)).isFalse();
  }

}

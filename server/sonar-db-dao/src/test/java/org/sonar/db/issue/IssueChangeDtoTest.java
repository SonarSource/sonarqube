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
package org.sonar.db.issue;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;

class IssueChangeDtoTest {

  @Test
  void create_from_comment() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");

    IssueChangeDto dto = IssueChangeDto.of(comment, "project_uuid");

    assertThat(dto.getChangeData()).isEqualTo("the comment");
    assertThat(dto.getChangeType()).isEqualTo("comment");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueChangeCreationDate()).isNotNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserUuid()).isEqualTo("user_uuid");
    assertThat(dto.getProjectUuid()).isEqualTo("project_uuid");
  }

  @Test
  void create_from_comment_with_created_at() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");
    comment.setCreatedAt(parseDate("2015-01-13"));

    IssueChangeDto dto = IssueChangeDto.of(comment, "project_uuid");

    assertThat(dto.getIssueChangeCreationDate()).isEqualTo(parseDate("2015-01-13").getTime());
  }

  @Test
  void create_from_diff() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "INFO", "BLOCKER");
    diffs.setUserUuid("user_uuid");
    diffs.setCreationDate(parseDate("2015-01-13"));

    IssueChangeDto dto = IssueChangeDto.of("ABCDE", diffs, "project_uuid");

    assertThat(dto.getChangeData()).isEqualTo("severity=INFO|BLOCKER");
    assertThat(dto.getChangeType()).isEqualTo("diff");
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();
    assertThat(dto.getIssueKey()).isEqualTo("ABCDE");
    assertThat(dto.getUserUuid()).isEqualTo("user_uuid");
    assertThat(dto.getIssueChangeCreationDate()).isEqualTo(parseDate("2015-01-13").getTime());
    assertThat(dto.getProjectUuid()).isEqualTo("project_uuid");
  }

  @Test
  void create_from_diff_with_created_at() {
    FieldDiffs diffs = new FieldDiffs();
    diffs.setDiff("severity", "INFO", "BLOCKER");
    diffs.setUserUuid("user_uuid");
    diffs.setCreationDate(parseDate("2015-01-13"));

    IssueChangeDto dto = IssueChangeDto.of("ABCDE", diffs, "project_uuid");

    assertThat(dto.getIssueChangeCreationDate()).isEqualTo(parseDate("2015-01-13").getTime());
  }

  @Test
  void to_comment() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserUuid("user_uuid")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setCreatedAt(System2.INSTANCE.now())
      .setUpdatedAt(System2.INSTANCE.now());

    DefaultIssueComment comment = changeDto.toComment();
    assertThat(comment.key()).isEqualTo("EFGH");
    assertThat(comment.markdownText()).isEqualTo("Some text");
    assertThat(comment.createdAt()).isNotNull();
    assertThat(comment.updatedAt()).isNotNull();
    assertThat(comment.userUuid()).isEqualTo("user_uuid");
    assertThat(comment.issueKey()).isEqualTo("ABCDE");
  }

  @Test
  void to_field_diffs_with_issue_creation_date() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserUuid("user_uuid")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setIssueChangeCreationDate(System2.INSTANCE.now());

    FieldDiffs diffs = changeDto.toFieldDiffs();
    assertThat(diffs.userUuid()).contains("user_uuid");
    assertThat(diffs.issueKey()).contains("ABCDE");
    assertThat(diffs.creationDate()).isNotNull();
  }

  @Test
  void to_field_diffs_with_create_at() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserUuid("user_uuid")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setCreatedAt(System2.INSTANCE.now());

    FieldDiffs diffs = changeDto.toFieldDiffs();
    assertThat(diffs.userUuid()).contains("user_uuid");
    assertThat(diffs.issueKey()).contains("ABCDE");
    assertThat(diffs.creationDate()).isNotNull();
  }

  @Test
  void getIssueChangeCreationDate_fallback_to_createAt_when_null() {
    IssueChangeDto changeDto = new IssueChangeDto()
      .setKey("EFGH")
      .setUserUuid("user_uuid")
      .setChangeData("Some text")
      .setIssueKey("ABCDE")
      .setCreatedAt(10_000_000L)
      .setUpdatedAt(20_000_000L);

    assertThat(changeDto.getIssueChangeCreationDate()).isEqualTo(10_000_000L);
  }

  @Test
  void to_string() {
    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "user_uuid", "the comment");
    IssueChangeDto dto = IssueChangeDto.of(comment, "project_uuid");
    assertThat(dto.toString()).contains("ABCDE");
  }
}

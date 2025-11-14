/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueFixedDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueFixedDao underTest = db.getDbClient().issueFixedDao();

  @Test
  void insert_shouldPersistFixedIssue() {
    IssueFixedDto fixedIssue = new IssueFixedDto("PR-1", "ISSUE-1");
    underTest.insert(db.getSession(), fixedIssue);
    assertThat(underTest.selectByPullRequest(db.getSession(), "PR-1")).containsOnly(fixedIssue);
  }

  @Test
  void insert_shouldThrowException_whenDuplicateRecord() {
    IssueFixedDto fixedIssue = new IssueFixedDto("PR-1", "ISSUE-1");
    DbSession session = db.getSession();
    underTest.insert(session, fixedIssue);

    assertThatThrownBy(() -> underTest.insert(session, fixedIssue))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void selectByPullRequest_shouldReturnAllFixedIssuesOfPullRequest() {
    IssueFixedDto fixedIssue1 = new IssueFixedDto("PR-1", "ISSUE-1");
    IssueFixedDto fixedIssue2 = new IssueFixedDto("PR-1", "ISSUE-2");
    IssueFixedDto fixedIssue3 = new IssueFixedDto("PR-2", "ISSUE-3");
    underTest.insert(db.getSession(), fixedIssue1);
    underTest.insert(db.getSession(), fixedIssue2);
    underTest.insert(db.getSession(), fixedIssue3);

    assertThat(underTest.selectByPullRequest(db.getSession(), "PR-1")).containsOnly(fixedIssue1, fixedIssue2);
  }

  @Test
  void delete_shouldRemovedExpectedIssuesFixed() {
    IssueFixedDto fixedIssue1 = new IssueFixedDto("PR-1", "ISSUE-1");
    IssueFixedDto fixedIssue2 = new IssueFixedDto("PR-1", "ISSUE-2");
    underTest.insert(db.getSession(), fixedIssue1);
    underTest.insert(db.getSession(), fixedIssue2);

    underTest.delete(db.getSession(), fixedIssue1);

    assertThat(underTest.selectByPullRequest(db.getSession(), "PR-1")).containsOnly(fixedIssue2);
  }

}

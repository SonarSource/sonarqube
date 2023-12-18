/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IssueFixedDaoIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueFixedDao underTest = db.getDbClient().issueFixedDao();

  @Test
  public void insert_shouldPersistFixedIssue() {
    IssueFixedDto fixedIssue = new IssueFixedDto("PR-1", "ISSUE-1");
    underTest.insert(db.getSession(), fixedIssue);
    assertThat(underTest.selectByPullRequest(db.getSession(), "PR-1")).containsOnly(fixedIssue);
  }

  @Test
  public void insert_shouldThrowException_whenDuplicateRecord() {
    IssueFixedDto fixedIssue = new IssueFixedDto("PR-1", "ISSUE-1");
    DbSession session = db.getSession();
    underTest.insert(session, fixedIssue);

    assertThatThrownBy(() -> underTest.insert(session, fixedIssue))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void selectByPullRequest_shouldReturnAllFixedIssuesOfPullRequest() {
    IssueFixedDto fixedIssue1 = new IssueFixedDto("PR-1", "ISSUE-1");
    IssueFixedDto fixedIssue2 = new IssueFixedDto("PR-1", "ISSUE-2");
    IssueFixedDto fixedIssue3 = new IssueFixedDto("PR-2", "ISSUE-3");
    underTest.insert(db.getSession(), fixedIssue1);
    underTest.insert(db.getSession(), fixedIssue2);
    underTest.insert(db.getSession(), fixedIssue3);

    assertThat(underTest.selectByPullRequest(db.getSession(), "PR-1")).containsOnly(fixedIssue1, fixedIssue2);
  }

}

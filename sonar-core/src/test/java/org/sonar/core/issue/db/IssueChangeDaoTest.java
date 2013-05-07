/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangeDaoTest extends AbstractDaoTestCase {

  IssueChangeDao dao;

  @Before
  public void setUp() {
    dao = new IssueChangeDao(getMyBatis());
  }

  @Test
  public void should_select_issue_comments() {
    setupData("shared");

    DefaultIssueComment[] comments = dao.selectIssueComments("1000");
    assertThat(comments).hasSize(2);

    // chronological order
    DefaultIssueComment first = comments[0];
    assertThat(first.text()).isEqualTo("old comment");


    DefaultIssueComment second = comments[1];
    assertThat(second.userLogin()).isEqualTo("arthur");
    assertThat(second.key()).isEqualTo("FGHIJ");
    assertThat(second.text()).isEqualTo("recent comment");
  }

  @Test
  public void should_select_issue_changes() {
    setupData("shared");

    FieldDiffs[] ordered = dao.selectIssueChanges("1000");
    assertThat(ordered).hasSize(1);
    FieldDiffs.Diff severityDiff = ordered[0].get("severity");
    assertThat(severityDiff.oldValue()).isEqualTo("MAJOR");
    assertThat(severityDiff.newValue()).isEqualTo("BLOCKER");
  }
}

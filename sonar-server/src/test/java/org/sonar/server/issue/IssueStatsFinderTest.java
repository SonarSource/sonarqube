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

package org.sonar.server.issue;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.db.IssueStatsDao;
import org.sonar.core.user.DefaultUser;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueStatsFinderTest {

  private IssueStatsDao issuestatsDao = mock(IssueStatsDao.class);
  private UserFinder userFinder = mock(UserFinder.class);

  @Test
  public void should_find_assignees(){
    when(issuestatsDao.selectIssuesColumn(any(IssueQuery.class), anyString(), anyInt())).thenReturn(Lists.<Object>newArrayList("perceval", "perceval", "arthur", null));
    when(userFinder.findByLogins(anyListOf(String.class))).thenReturn(Lists.<User>newArrayList(
      new DefaultUser().setLogin("perceval").setName("Perceval"),
      new DefaultUser().setLogin("arthur").setName("Roi Arthur")
    ));

    IssueStatsFinder issueStatsFinder = new IssueStatsFinder(issuestatsDao, userFinder);
    IssueStatsFinder.IssueStatsResult issueStatsResult = issueStatsFinder.findIssueAssignees(IssueQuery.builder().build());
    assertThat(issueStatsResult.results()).hasSize(4);
    assertThat(issueStatsResult.user("arthur").name()).isEqualTo("Roi Arthur");
  }
}

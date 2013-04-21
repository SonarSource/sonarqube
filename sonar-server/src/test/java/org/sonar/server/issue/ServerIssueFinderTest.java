/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.issue;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.AuthorizationDao;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerIssueFinderTest {

  MyBatis mybatis;
  ServerIssueFinder finder;
  IssueDao issueDao;
  AuthorizationDao authorizationDao;

  @Before
  public void before() {
    mybatis = mock(MyBatis.class);
    issueDao = mock(IssueDao.class);
    authorizationDao = mock(AuthorizationDao.class);
    finder = new ServerIssueFinder(mybatis, issueDao, authorizationDao);
  }

  @Test
  public void should_find_issues() {
    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.select(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);

    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
  }

  @Test
  public void should_find_by_key() {
    IssueDto issueDto = new IssueDto().setId(1L).setRuleId(1).setResourceId(1)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle");
    when(issueDao.selectByKey("ABCDE")).thenReturn(issueDto);

    Issue issue = finder.findByKey("ABCDE");
    assertThat(issue).isNotNull();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
  }

  private void grantAccessRights() {
    when(authorizationDao.keepAuthorizedComponentIds(anySet(), anyInt(), anyString(), any(SqlSession.class)))
      .thenAnswer(new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
          return invocationOnMock.getArguments()[0];
        }
      });
  }
}

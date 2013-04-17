/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.issue;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.AuthorizationDao;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIssueFinderTest {

  MyBatis mybatis;
  DefaultIssueFinder finder;
  IssueDao issueDao;
  ResourceDao resourceDao;
  RuleFinder ruleFinder;
  AuthorizationDao authorizationDao;

  @Before
  public void before() {
    mybatis = mock(MyBatis.class);
    issueDao = mock(IssueDao.class);
    resourceDao = mock(ResourceDao.class);
    ruleFinder = mock(RuleFinder.class);
    authorizationDao = mock(AuthorizationDao.class);
    finder = new DefaultIssueFinder(mybatis, issueDao, resourceDao, authorizationDao, ruleFinder);
  }

  @Test
  public void should_find_issues() {
    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123);
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123);
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.select(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    Rule rule = Rule.create("repo", "key");
    rule.setId(50);
    when(ruleFinder.findById(anyInt())).thenReturn(rule);
    when(resourceDao.getResource(anyInt())).thenReturn(new ResourceDto().setKey("componentKey").setId(123L));

    IssueFinder.Results results = finder.find(issueQuery, null);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("componentKey");
    assertThat(issue.ruleKey().toString()).isEqualTo("repo:key");
  }

  @Test
  public void should_find_by_key() {
    IssueDto issueDto = new IssueDto().setId(1L).setRuleId(1).setResourceId(1);
    when(issueDao.selectByKey("ABCDE")).thenReturn(issueDto);
    when(ruleFinder.findById(anyInt())).thenReturn(Rule.create("squid", "NullDeref"));
    when(resourceDao.getResource(anyInt())).thenReturn(new ResourceDto().setKey("org/struts/Action.java"));

    Issue issue = finder.findByKey("ABCDE");
    assertThat(issue).isNotNull();
    assertThat(issue.componentKey()).isEqualTo("org/struts/Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:NullDeref");
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

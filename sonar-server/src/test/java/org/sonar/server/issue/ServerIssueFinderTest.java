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
package org.sonar.server.issue;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.rules.Rule;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.user.AuthorizationDao;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ServerIssueFinderTest {

  MyBatis mybatis;
  ServerIssueFinder finder;

  IssueDao issueDao;
  AuthorizationDao authorizationDao;
  DefaultRuleFinder ruleFinder;
  ResourceDao resourceDao;

  @Before
  public void before() {
    mybatis = mock(MyBatis.class);
    issueDao = mock(IssueDao.class);
    authorizationDao = mock(AuthorizationDao.class);
    ruleFinder = mock(DefaultRuleFinder.class);
    resourceDao = mock(ResourceDao.class);
    finder = new ServerIssueFinder(mybatis, issueDao, authorizationDao, ruleFinder, resourceDao);
  }

  @Test
  public void should_find_issues() {
    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
    assertThat(results.securityExclusions()).isFalse();
  }

  @Test
  public void should_find_only_authorized_issues() {
    IssueQuery issueQuery = mock(IssueQuery.class);
    when(issueQuery.pageSize()).thenReturn(100);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(135)
      .setComponentKey_unit_test_only("Phases.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    when(authorizationDao.keepAuthorizedComponentIds(anySet(), anyInt(), anyString(), any(SqlSession.class))).thenReturn(newHashSet(123));
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(newArrayList(issue1));

    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);

    verify(issueDao).selectByIds(eq(newHashSet(1L)), any(SqlSession.class));
    assertThat(results.securityExclusions()).isTrue();
  }

  @Test
  public void should_find_paginate_result() {
    grantAccessRights();

    IssueQuery issueQuery = mock(IssueQuery.class);
    when(issueQuery.pageSize()).thenReturn(1);
    when(issueQuery.pageIndex()).thenReturn(1);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(135)
      .setComponentKey_unit_test_only("Phases.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.paging().offset()).isEqualTo(0);
    assertThat(results.paging().total()).isEqualTo(2);
    assertThat(results.paging().pages()).isEqualTo(2);

    // Only one result is expected because the limit is 1
    verify(issueDao).selectByIds(eq(newHashSet(1L)), any(SqlSession.class));
  }

  @Test
  public void should_find_by_key() {
    IssueDto issueDto = new IssueDto().setId(1L).setRuleId(1).setResourceId(1)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    when(issueDao.selectByKey("ABCDE")).thenReturn(issueDto);

    Issue issue = finder.findByKey("ABCDE");
    assertThat(issue).isNotNull();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
  }

  @Test
  public void should_get_rule_from_result() {
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    when(ruleFinder.findByIds(anyCollection())).thenReturn(newArrayList(rule));

    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(results.issues()).hasSize(2);
    assertThat(results.rule(issue)).isEqualTo(rule);
    assertThat(results.rules()).hasSize(1);
  }

  @Test
  public void should_get_component_from_result() {
    Component component = new ComponentDto().setKey("Action.java");
    when(resourceDao.findByIds(anyCollection())).thenReturn(newArrayList(component));

    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setResourceId(123)
      .setComponentKey_unit_test_only("Action.java")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(dtoList);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);


    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(results.issues()).hasSize(2);
    assertThat(results.component(issue)).isEqualTo(component);
    assertThat(results.components()).hasSize(1);
  }

  @Test
  public void should_get_empty_rule_and_component_from_result_when_no_issue() {
    grantAccessRights();
    IssueQuery issueQuery = mock(IssueQuery.class);
    when(issueDao.selectIssueIdsAndComponentsId(eq(issueQuery), any(SqlSession.class))).thenReturn(Collections.<IssueDto>emptyList());
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(Collections.<IssueDto>emptyList());


    IssueFinder.Results results = finder.find(issueQuery, null, UserRole.USER);
    assertThat(results.issues()).isEmpty();
    assertThat(results.rules()).isEmpty();
    assertThat(results.components()).isEmpty();
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

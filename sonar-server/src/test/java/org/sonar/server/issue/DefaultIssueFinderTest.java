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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.rules.Rule;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.user.DefaultUser;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DefaultIssueFinderTest {

  MyBatis mybatis = mock(MyBatis.class);
  IssueDao issueDao = mock(IssueDao.class);
  IssueChangeDao issueChangeDao = mock(IssueChangeDao.class);
  DefaultRuleFinder ruleFinder = mock(DefaultRuleFinder.class);
  ResourceDao resourceDao = mock(ResourceDao.class);
  ActionPlanService actionPlanService = mock(ActionPlanService.class);
  UserFinder userFinder = mock(UserFinder.class);
  DefaultIssueFinder finder = new DefaultIssueFinder(mybatis, issueDao, issueChangeDao, ruleFinder, userFinder, resourceDao, actionPlanService);

  @Test
  public void find_issues() {
    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    verify(issueDao).selectIssueIds(eq(query), anyInt(), any(SqlSession.class));

    assertThat(results.issues()).hasSize(2);
    DefaultIssue issue = (DefaultIssue) results.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.projectKey()).isEqualTo("struts");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
  }

  @Test
  public void find_paginate_result() {
    IssueQuery query = IssueQuery.builder().pageSize(1).pageIndex(1).build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(135l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Phases.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectIssueIds(eq(query), anyInt(), any(SqlSession.class))).thenReturn(dtoList);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    assertThat(results.paging().offset()).isEqualTo(0);
    assertThat(results.paging().total()).isEqualTo(2);
    assertThat(results.paging().pages()).isEqualTo(2);

    // Only one result is expected because the limit is 1
    verify(issueDao).selectByIds(eq(newHashSet(1L)), any(SqlSession.class));
  }

  @Test
  public void find_by_key() {
    IssueDto issueDto = new IssueDto().setId(1L).setRuleId(1).setComponentId(1l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    when(issueDao.selectByKey("ABCDE")).thenReturn(issueDto);

    Issue issue = finder.findByKey("ABCDE");
    assertThat(issue).isNotNull();
    assertThat(issue.componentKey()).isEqualTo("Action.java");
    assertThat(issue.ruleKey().toString()).isEqualTo("squid:AvoidCycle");
  }

  @Test
  public void get_rule_from_result() {
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    when(ruleFinder.findByIds(anyCollection())).thenReturn(newArrayList(rule));

    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(results.issues()).hasSize(2);
    assertThat(results.rule(issue)).isEqualTo(rule);
    assertThat(results.rules()).hasSize(1);
  }

  @Test
  public void get_no_rule_from_result_with_hide_rules_param() {
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    when(ruleFinder.findByIds(anyCollection())).thenReturn(newArrayList(rule));

    IssueQuery query = IssueQuery.builder().hideRules(true).build();

    IssueDto issue = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(newArrayList(issue));

    IssueQueryResult results = finder.find(query);
    Issue result = results.issues().iterator().next();
    assertThat(results.rule(result)).isNull();
    assertThat(results.rules()).isEmpty();
  }

  @Test
  public void get_component_from_result() {
    Component component = new ComponentDto().setKey("Action.java");
    when(resourceDao.findByIds(anyCollection())).thenReturn(newArrayList(component));

    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).hasSize(2);
    assertThat(results.components()).hasSize(1);
    Issue issue = results.issues().iterator().next();
    assertThat(results.component(issue)).isEqualTo(component);
  }

  @Test
  public void get_project_from_result() {
    Component project = new ComponentDto().setKey("struts");
    when(resourceDao.findByIds(anyCollection())).thenReturn(newArrayList(project));

    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).hasSize(2);
    assertThat(results.projects()).hasSize(1);
    Issue issue = results.issues().iterator().next();
    assertThat(results.project(issue)).isEqualTo(project);
  }

  @Test
  public void get_action_plans_from_result() {
    ActionPlan actionPlan1 = DefaultActionPlan.create("Short term").setKey("A");
    ActionPlan actionPlan2 = DefaultActionPlan.create("Long term").setKey("B");

    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l).setKee("ABC").setActionPlanKey("A")
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l).setKee("DEF").setActionPlanKey("B")
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);
    when(actionPlanService.findByKeys(anyCollection())).thenReturn(newArrayList(actionPlan1, actionPlan2));

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).hasSize(2);
    assertThat(results.actionPlans()).hasSize(2);
    Issue issue = results.issues().iterator().next();
    assertThat(results.actionPlan(issue)).isNotNull();
  }

  @Test
  public void get_user_from_result() {
    when(userFinder.findByLogins(anyListOf(String.class))).thenReturn(Lists.<User>newArrayList(
      new DefaultUser().setLogin("perceval").setName("Perceval"),
      new DefaultUser().setLogin("arthur").setName("Roi Arthur")
      ));

    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue1 = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l).setKee("ABC").setAssignee("perceval")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    IssueDto issue2 = new IssueDto().setId(2L).setRuleId(50).setComponentId(123l).setRootComponentId(100l).setKee("DEF").setReporter("arthur")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN");
    List<IssueDto> dtoList = newArrayList(issue1, issue2);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).hasSize(2);

    assertThat(results.users()).hasSize(2);
    assertThat(results.user("perceval").name()).isEqualTo("Perceval");
    assertThat(results.user("arthur").name()).isEqualTo("Roi Arthur");
  }

  @Test
  public void get_empty_result_when_no_issue() {
    IssueQuery query = IssueQuery.builder().build();
    when(issueDao.selectIssueIds(eq(query), anyInt(), any(SqlSession.class))).thenReturn(Collections.<IssueDto>emptyList());
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(Collections.<IssueDto>emptyList());

    IssueQueryResult results = finder.find(query);
    assertThat(results.issues()).isEmpty();
    assertThat(results.rules()).isEmpty();
    assertThat(results.components()).isEmpty();
    assertThat(results.actionPlans()).isEmpty();
  }

  @Test
  public void find_issue_with_technical_debt() {
    IssueQuery query = IssueQuery.builder().build();

    IssueDto issue = new IssueDto().setId(1L).setRuleId(50).setComponentId(123l).setRootComponentId(100l)
      .setComponentKey_unit_test_only("Action.java")
      .setRootComponentKey_unit_test_only("struts")
      .setRuleKey_unit_test_only("squid", "AvoidCycle")
      .setStatus("OPEN").setResolution("OPEN")
      .setTechnicalDebt(10L);
    List<IssueDto> dtoList = newArrayList(issue);
    when(issueDao.selectByIds(anyCollection(), any(SqlSession.class))).thenReturn(dtoList);

    IssueQueryResult results = finder.find(query);
    verify(issueDao).selectIssueIds(eq(query), anyInt(), any(SqlSession.class));

    assertThat(results.issues()).hasSize(1);
    DefaultIssue result = (DefaultIssue) results.issues().iterator().next();
    assertThat(result.technicalDebt()).isEqualTo(WorkDayDuration.of(10, 0, 0));
  }

}

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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueResult;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class IssueServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  IssueService service;

  RuleDto rule;
  ComponentDto project;
  ComponentDto resource;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    service = tester.get(IssueService.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = new ComponentDto()
      .setEnabled(true)
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(session, project);

    resource = new ComponentDto()
      .setEnabled(true)
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    tester.get(ComponentDao.class).insert(session, resource);
    session.commit();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void can_facet() throws Exception {
    IssueDto issue1 = getIssue().setActionPlanKey("P1");
    IssueDto issue2 = getIssue().setActionPlanKey("P2");
    tester.get(IssueDao.class).insert(session, issue1, issue2);
    session.commit();

    IssueResult result = service.search(IssueQuery.builder().build(), new QueryOptions());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getFacets()).isEmpty();

    result = service.search(IssueQuery.builder().build(), new QueryOptions().setFacet(true));
    assertThat(result.getFacets().keySet()).hasSize(4);
    assertThat(result.getFacetKeys("actionPlan")).hasSize(2);
  }

  @Test
  public void has_component_and_project() throws Exception {
    IssueDto issue1 = getIssue().setActionPlanKey("P1");
    IssueDto issue2 = getIssue().setActionPlanKey("P2");
    tester.get(IssueDao.class).insert(session, issue1, issue2);
    session.commit();

    IssueResult result = service.search(IssueQuery.builder().build(), new QueryOptions());
    assertThat(result.projects()).hasSize(1);
    assertThat(result.components()).hasSize(1);
  }

  private IssueDto getIssue() {
    return new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setKee(UUID.randomUUID().toString());
  }
}

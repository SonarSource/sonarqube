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
package org.sonar.data.issues;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class IssueData {

  public final static int MAX_NUMBER_RULES = 2500;
  public final static int MAX_NUMBER_PROJECTS = 500;
  public final static int MAX_NUMBER_RESOURCES_PER_PROJECT = 10000;

  public final static int ISSUE_COUNT = 1000000;

  protected static final Logger LOGGER = LoggerFactory.getLogger(IssueData.class);

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @After
  public void tearDown() throws Exception {
    tester.clearDbAndIndexes();
    if (session != null) {
      session.close();
    }
  }

  @AfterClass
  public static void reset() throws Exception {
    tester = new ServerTester();
  }

  protected Random generator = new Random(System.currentTimeMillis());
  protected DbClient db = tester.get(DbClient.class);
  protected IndexClient index = tester.get(IndexClient.class);
  protected DbSession session = tester.get(DbClient.class).openSession(true);

  protected List<RuleDto> rules = new ArrayList<RuleDto>();
  protected Map<Long, List<Long>> projects = new HashMap<Long, List<Long>>();

  protected IssueDto getIssue(int id) {
    RuleDto rule = rules.get(generator.nextInt(rules.size()));
    Long projectId = Iterables.get(projects.keySet(), generator.nextInt(projects.size()));
    Long resourceId = projects.get(projectId).get(generator.nextInt(projects.get(projectId).size()));
    return new IssueDto().setId(new Long(id))
      .setRootComponentId(projectId)
      .setRootComponentKey(projectId + "_key")
      .setComponentId(resourceId)
      .setComponentKey(resourceId + "_key")
      .setRule(rule)
      .setMessage("Lorem ipsum loertium bortim tata toto tutu 14 failures in this issue")
      .setAssignee("assignee_")
      .setSeverity("BLOCKER")
      .setReporter("Luc besson")
      .setAuthorLogin("Pinpin")
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
  }

  protected void generateRules(DbSession dbSession) {
    // Generate Rules
    for (int i = 0; i < MAX_NUMBER_RULES; i++) {
      rules.add(RuleTesting.newDto(RuleKey.of("data_repo", "S" + i)));
    }
    DbSession setupSession = db.openSession(false);
    db.ruleDao().insert(setupSession, rules);
    setupSession.commit();
  }

  protected void generateProjects(DbSession setupSession) {
    // Generate projects & resources
    for(long p = 1; p<=MAX_NUMBER_PROJECTS; p++) {
      ComponentDto project = new ComponentDto()
        .setId(p)
        .setKey("MyProject")
        .setProjectId_unit_test_only(p);
      db.componentDao().insert(setupSession, project);
      projects.put(project.projectId(), new ArrayList<Long>());
      List<ComponentDto> resources = new ArrayList<ComponentDto>();
      for(int i = 0; i<generator.nextInt(MAX_NUMBER_RESOURCES_PER_PROJECT); i++) {
        ComponentDto resource = new ComponentDto()
          .setKey("MyComponent_"+(p*MAX_NUMBER_PROJECTS+i))
          .setProjectId_unit_test_only(project.getId());
        db.componentDao().insert(setupSession, resource);
        resources.add(resource);
      }
      setupSession.commit();
      for (ComponentDto resource : resources) {
        projects.get(project.projectId()).add(resource.getId());
      }
    }
  }

  protected int documentPerSecond(long time) {
    return (int)Math.round(ISSUE_COUNT/(time/1000.0));
  }
}

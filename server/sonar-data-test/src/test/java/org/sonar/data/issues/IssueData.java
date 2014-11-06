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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IssueData {

  public final static int MAX_NUMBER_RULES = 2500;
  public final static int MAX_NUMBER_PROJECTS = 500;
  public final static int MAX_NUMBER_RESOURCES_PER_PROJECT = 10000;

  public final static int ISSUE_COUNT = 100000;

  protected static final Logger LOGGER = LoggerFactory.getLogger(IssueData.class);

  @ClassRule
  public static ServerTester tester = new ServerTester();

  protected Random generator = new Random(System.currentTimeMillis());
  protected DbClient db = tester.get(DbClient.class);
  protected IndexClient index = tester.get(IndexClient.class);
  protected DbSession session = tester.get(DbClient.class).openSession(true);

  protected List<RuleDto> rules = new ArrayList<RuleDto>();
  protected ArrayListMultimap<ComponentDto, ComponentDto> componentsByProjectId = ArrayListMultimap.create();

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

  protected IssueDto getIssue(int id) {
    RuleDto rule = rules.get(generator.nextInt(rules.size()));
    ComponentDto project = Iterables.get(componentsByProjectId.keySet(), generator.nextInt(componentsByProjectId.keySet().size()));
    ComponentDto file = componentsByProjectId.get(project).get(generator.nextInt(componentsByProjectId.get(project).size()));

    return IssueTesting.newDto(rule, file, project)
      .setMessage("Lorem ipsum loertium bortim tata toto tutu 14 failures in this issue")
      .setAssignee("assignee_")
      .setSeverity(Severity.BLOCKER)
      .setReporter("Luc besson")
      .setAuthorLogin("Pinpin")
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED);
  }

  protected void generateRules(DbSession dbSession) {
    // Generate Rules
    for (int i = 0; i < MAX_NUMBER_RULES; i++) {
      rules.add(RuleTesting.newDto(RuleKey.of("rule-repo", "rule-key-" + i)));
    }
    DbSession setupSession = db.openSession(false);
    db.ruleDao().insert(setupSession, rules);
    setupSession.commit();
  }

  protected void generateProjects(DbSession setupSession) {
    // Generate projects & resources
    for (long p = 1; p <= MAX_NUMBER_PROJECTS; p++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setKey("project-" + p)
        .setName("Project " + p)
        .setLongName("Project " + p);
      db.componentDao().insert(setupSession, project);

      for (int i = 0; i < generator.nextInt(MAX_NUMBER_RESOURCES_PER_PROJECT); i++) {
        ComponentDto file = ComponentTesting.newFileDto(project)
          .setKey("file-" + (p * MAX_NUMBER_PROJECTS + i))
          .setName("File " + (p * MAX_NUMBER_PROJECTS + i))
          .setLongName("File " + (p * MAX_NUMBER_PROJECTS + i));
        db.componentDao().insert(setupSession, file);
        componentsByProjectId.put(project, file);
      }
      setupSession.commit();
    }
  }

  protected int documentPerSecond(long time) {
    return (int) Math.round(ISSUE_COUNT / (time / 1000.0));
  }
}

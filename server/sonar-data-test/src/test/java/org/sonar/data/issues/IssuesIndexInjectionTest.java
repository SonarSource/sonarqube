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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.InsertDto;
import org.sonar.server.search.action.RefreshIndex;
import org.sonar.server.tester.ServerTester;

import java.util.List;
import java.util.Timer;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssuesIndexInjectionTest extends AbstractTest {

  static final Logger LOGGER = LoggerFactory.getLogger(IssuesIndexInjectionTest.class);

  final static int PROJECTS_NUMBER = 100;
  final static int NUMBER_FILES_PER_PROJECT = 100;
  final static int NUMBER_ISSUES_PER_FILE = 100;

  final static int ISSUE_COUNT = PROJECTS_NUMBER * NUMBER_FILES_PER_PROJECT * NUMBER_ISSUES_PER_FILE;

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbSession batchSession;

  IssueIndex issueIndex;

  List<ComponentDto> projects = newArrayList();
  ArrayListMultimap<ComponentDto, ComponentDto> componentsByProjectId = ArrayListMultimap.create();

  @Before
  public void setUp() throws Exception {
    issueIndex = tester.get(IssueIndex.class);

    batchSession = tester.get(DbClient.class).openSession(true);
  }

  @After
  public void after() throws Exception {
    batchSession.close();
  }

  @Test
  public void inject_issues() throws Exception {
    generateData();

    Timer timer = new Timer("Inject Issues");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);
    try {
      long start = System.currentTimeMillis();
      for (ComponentDto project : projects) {
        for (ComponentDto file : componentsByProjectId.get(project)) {
          for (int issueIndex = 1; issueIndex < NUMBER_ISSUES_PER_FILE + 1; issueIndex++) {
            batchSession.enqueue(new InsertDto<IssueDto>(IndexDefinition.ISSUES.getIndexType(), newIssue(issueIndex, file, project, rules.next()), false));
            counter.getAndIncrement();
          }
        }
      }
      batchSession.enqueue(new RefreshIndex(IndexDefinition.ISSUES.getIndexType()));
      batchSession.commit();
      long stop = System.currentTimeMillis();
      progressTask.log();

      assertThat(issueIndex.countAll()).isEqualTo(ISSUE_COUNT);

      long time = stop - start;
      LOGGER.info("Processed {} Issues in {} ms with avg {} Issue/second", ISSUE_COUNT, time, documentPerSecond(time));
      assertDurationAround(time, Long.parseLong(getProperty("IssuesIndexInjectionTest.inject_issues")));

    } finally {
      timer.cancel();
      timer.purge();
    }
  }

  private void generateData() {
    long ids = 1;

    for (int i = 0; i < RULES_NUMBER; i++) {
      rules.next().setId((int) ids++);
    }

    long start = System.currentTimeMillis();
    for (long projectIndex = 0; projectIndex < PROJECTS_NUMBER; projectIndex++) {
      ComponentDto project = ComponentTesting.newProjectDto()
        .setId(ids++)
        .setKey("project-" + projectIndex)
        .setName("Project " + projectIndex)
        .setLongName("Project " + projectIndex);
      projects.add(project);

      for (int fileIndex = 0; fileIndex < NUMBER_FILES_PER_PROJECT; fileIndex++) {
        String index = projectIndex * PROJECTS_NUMBER + fileIndex + "";
        ComponentDto file = ComponentTesting.newFileDto(project)
          .setId(ids++)
          .setKey("file-" + index)
          .setName("File " + index)
          .setLongName("File " + index);
        componentsByProjectId.put(project, file);
      }
    }
    LOGGER.info("Generated data in {} ms", System.currentTimeMillis() - start);
  }

  protected int documentPerSecond(long time) {
    return (int) Math.round(ISSUE_COUNT / (time / 1000.0));
  }

}

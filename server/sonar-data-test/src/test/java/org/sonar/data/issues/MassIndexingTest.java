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

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.InsertDto;
import org.sonar.server.search.action.RefreshIndex;

import static org.fest.assertions.Assertions.assertThat;

public class MassIndexingTest extends IssueData {

  @Before
  public void setUp() throws Exception {
    DbSession setupSession = db.openSession(false);
    generateRules(setupSession);
    generateProjects(setupSession);
    setupSession.commit();
    setupSession.close();
  }

  @Test
  public void enqueue_issues() throws Exception {

    long start = System.currentTimeMillis();
    int issueInsertCount = ISSUE_COUNT;
    for (int i = 0; i < issueInsertCount; i++) {
      session.enqueue(new InsertDto<IssueDto>(IndexDefinition.ISSUES.getIndexType(), getIssue(i), false));
    }
    session.enqueue(new RefreshIndex(IndexDefinition.ISSUES.getIndexType()));
    session.commit();
    long stop = System.currentTimeMillis();

    //TODO add performance assertions here
    assertThat(index.get(IssueIndex.class).countAll()).isEqualTo(issueInsertCount);

    long time = stop-start;
    LOGGER.info("processed {} Issues in {}ms with avg {} Issue/second", ISSUE_COUNT, time, this.documentPerSecond(time));

  }
}

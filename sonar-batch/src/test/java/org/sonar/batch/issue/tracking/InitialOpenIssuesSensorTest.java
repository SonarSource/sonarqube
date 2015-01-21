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
package org.sonar.batch.issue.tracking;

import org.apache.ibatis.session.ResultHandler;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InitialOpenIssuesSensorTest {

  InitialOpenIssuesStack stack = mock(InitialOpenIssuesStack.class);
  IssueDao issueDao = mock(IssueDao.class);
  IssueChangeDao issueChangeDao = mock(IssueChangeDao.class);

  InitialOpenIssuesSensor sensor = new InitialOpenIssuesSensor(stack, issueDao, issueChangeDao);

  @Test
  public void should_select_module_open_issues() {
    Project project = new Project("key");
    project.setId(1);
    sensor.analyse(project, null);

    verify(issueDao).selectNonClosedIssuesByModule(eq(1), any(ResultHandler.class));
  }

  @Test
  public void should_select_module_open_issues_changelog() {
    Project project = new Project("key");
    project.setId(1);
    sensor.analyse(project, null);

    verify(issueChangeDao).selectChangelogOnNonClosedIssuesByModuleAndType(eq(1), any(ResultHandler.class));
  }

  @Test
  public void test_toString() throws Exception {
    assertThat(sensor.toString()).isEqualTo("InitialOpenIssuesSensor");

  }
}

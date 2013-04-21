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

package org.sonar.batch.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InitialOpenIssuesSensorTest {

  private InitialOpenIssuesSensor initialOpenIssuesSensor;
  private InitialOpenIssuesStack initialOpenIssuesStack;
  private IssueDao issueDao;

  @Before
  public void before() {
    initialOpenIssuesStack = mock(InitialOpenIssuesStack.class);
    issueDao = mock(IssueDao.class);

    initialOpenIssuesSensor = new InitialOpenIssuesSensor(initialOpenIssuesStack, issueDao);
  }

  @Test
  public void should_analyse() {
    Project project = new Project("key");
    project.setId(1);
    initialOpenIssuesSensor.analyse(project, null);

    verify(issueDao).selectOpenIssues(1);
    verify(initialOpenIssuesStack).setIssues(anyListOf(IssueDto.class), any(Date.class));
  }
}

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

package org.sonar.batch.issue;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.IssueDao;
import org.sonar.core.issue.IssueDto;

import java.util.List;

public class InitialOpenIssuesSensor implements Sensor {

  private final InitialOpenIssuesStack initialOpenIssuesStack;
  private final IssueDao issueDao;

  public InitialOpenIssuesSensor(InitialOpenIssuesStack initialOpenIssuesStack, IssueDao issueDao) {
    this.initialOpenIssuesStack = initialOpenIssuesStack;
    this.issueDao = issueDao;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    List<IssueDto> issuesDto = issueDao.selectOpenIssues(project.getId());
    initialOpenIssuesStack.setIssues(issuesDto);
  }

}

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
package org.sonar.plugins.core.issue;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;

import java.util.Date;
import java.util.List;

/**
 * Load all the issues referenced during the previous scan.
 */
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
    Date loadingDate = new Date();
    List<IssueDto> dtos = issueDao.selectNonClosedIssuesByModule(project.getId());
    initialOpenIssuesStack.setIssues(dtos, loadingDate);
  }

}

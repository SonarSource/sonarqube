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

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

public class ServerIssueStorage extends IssueStorage implements ServerComponent {

  private final ResourceDao resourceDao;

  public ServerIssueStorage(MyBatis mybatis, RuleFinder ruleFinder, ResourceDao resourceDao) {
    super(mybatis, ruleFinder);
    this.resourceDao = resourceDao;
  }

  @Override
  protected int componentId(DefaultIssue issue) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(issue.componentKey()));
    if (resourceDto == null) {
      throw new IllegalStateException("Unknown component: " + issue.componentKey());
    }
    return resourceDto.getId().intValue();
  }

  @Override
  protected int projectId(DefaultIssue issue) {
    ResourceDto resourceDto = resourceDao.getRootProjectByComponentKey(issue.componentKey());
    if (resourceDto == null) {
      throw new IllegalStateException("Unknown component: " + issue.componentKey());
    }
    return resourceDto.getId().intValue();
  }
}

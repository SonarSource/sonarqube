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
package org.sonar.batch.issue;

import org.sonar.api.BatchComponent;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

public class ScanIssueStorage extends IssueStorage implements BatchComponent {

  private final SnapshotCache snapshotCache;
  private final ResourceDao resourceDao;

  public ScanIssueStorage(MyBatis mybatis, RuleFinder ruleFinder, SnapshotCache snapshotCache, ResourceDao resourceDao) {
    super(mybatis, ruleFinder);
    this.snapshotCache = snapshotCache;
    this.resourceDao = resourceDao;
  }

  @Override
  protected int componentId(DefaultIssue issue) {
    Snapshot snapshot = getSnapshot(issue);
    if (snapshot != null) {
      return snapshot.getResourceId();
    }

    // Load from db when component does not exist in cache (deleted file for example)
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(issue.componentKey()));
    if (resourceDto == null) {
      throw new IllegalStateException("Unknown component: " + issue.componentKey());
    }
    return resourceDto.getId().intValue();
  }

  @Override
  protected int projectId(DefaultIssue issue) {
    Snapshot snapshot = getSnapshot(issue);
    if (snapshot != null) {
      return snapshot.getRootProjectId();
    }
    throw new IllegalStateException("Unknown component: " + issue.componentKey());
  }

  private Snapshot getSnapshot(DefaultIssue issue) {
    Snapshot snapshot = snapshotCache.get(issue.componentKey());
    if (snapshot != null) {
      return snapshot;
    }
    return null;
  }

}

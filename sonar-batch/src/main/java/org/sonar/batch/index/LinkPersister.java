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
package org.sonar.batch.index;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;

public final class LinkPersister {
  private DatabaseSession session;
  private ResourcePersister resourcePersister;

  public LinkPersister(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public void saveLink(Project project, ProjectLink link) {
    Snapshot snapshot = resourcePersister.getSnapshotOrFail(project);
    ResourceModel projectDao = session.reattach(ResourceModel.class, snapshot.getResourceId());
    ProjectLink dbLink = projectDao.getProjectLink(link.getKey());
    if (dbLink == null) {
      link.setResource(projectDao);
      projectDao.getProjectLinks().add(link);
      session.save(link);

    } else {
      dbLink.copyFieldsFrom(link);
      session.save(dbLink);
    }
    session.commit();

  }

  public void deleteLink(Project project, String linkKey) {
    Snapshot snapshot = resourcePersister.getSnapshot(project);
    if (snapshot != null) {
      ResourceModel model = session.reattach(ResourceModel.class, snapshot.getResourceId());
      ProjectLink dbLink = model.getProjectLink(linkKey);
      if (dbLink != null) {
        session.remove(dbLink);
        model.getProjectLinks().remove(dbLink);
        session.commit();
      }

    }
  }
}

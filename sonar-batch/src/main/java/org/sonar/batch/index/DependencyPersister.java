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
package org.sonar.batch.index;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.design.DependencyDto;
import org.sonar.api.resources.Project;

public final class DependencyPersister {

  private ResourcePersister resourcePersister;
  private DatabaseSession session;

  public DependencyPersister(ResourcePersister resourcePersister, DatabaseSession session) {
    this.resourcePersister = resourcePersister;
    this.session = session;
  }

  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
    Snapshot fromSnapshot = resourcePersister.saveResource(project, dependency.getFrom());
    Snapshot toSnapshot = resourcePersister.saveResource(project, dependency.getTo());
    Snapshot projectSnapshot = resourcePersister.getSnapshot(project);

    DependencyDto model = new DependencyDto();
    model.setProjectSnapshotId(projectSnapshot.getId());
    model.setUsage(dependency.getUsage());
    model.setWeight(dependency.getWeight());

    model.setFromResourceId(fromSnapshot.getResourceId());
    model.setFromScope(fromSnapshot.getScope());
    model.setFromSnapshotId(fromSnapshot.getId());

    model.setToResourceId(toSnapshot.getResourceId());
    model.setToSnapshotId(toSnapshot.getId());
    model.setToScope(toSnapshot.getScope());

    if (parentDependency != null) {
      // assume that it has been previously saved
      model.setParentDependencyId(parentDependency.getId());
    }
    session.save(model);
    dependency.setId(model.getId());
  }
}

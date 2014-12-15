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
import org.sonar.api.design.Dependency;
import org.sonar.api.design.DependencyDto;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public final class DependencyPersister {

  private ResourceCache resourceCache;
  private DatabaseSession session;

  public DependencyPersister(ResourceCache resourceCache, DatabaseSession session) {
    this.resourceCache = resourceCache;
    this.session = session;
  }

  public void saveDependency(Project project, Resource from, Resource to, Dependency dependency, Dependency parentDependency) {
    BatchResource fromResource = resourceCache.get(from);
    BatchResource toResource = resourceCache.get(to);
    BatchResource projectResource = resourceCache.get(project);

    DependencyDto model = new DependencyDto();
    model.setProjectSnapshotId(projectResource.snapshotId());
    model.setUsage(dependency.getUsage());
    model.setWeight(dependency.getWeight());

    model.setFromResourceId(fromResource.resource().getId());
    model.setFromScope(fromResource.resource().getScope());
    model.setFromSnapshotId(fromResource.snapshotId());

    model.setToResourceId(toResource.resource().getId());
    model.setToScope(toResource.resource().getScope());
    model.setToSnapshotId(toResource.snapshotId());

    if (parentDependency != null) {
      // assume that it has been previously saved
      model.setParentDependencyId(parentDependency.getId());
    }
    session.save(model);
    dependency.setId(model.getId());
  }
}

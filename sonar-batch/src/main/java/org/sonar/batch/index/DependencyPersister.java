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

import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.design.Dependency;
import org.sonar.api.design.DependencyDto;
import org.sonar.api.resources.Project;
import org.sonar.batch.dependency.DependencyCache;

import javax.annotation.Nullable;

public final class DependencyPersister {

  private final ResourceCache resourceCache;
  private final DatabaseSession session;
  private final DependencyCache dependencyCache;

  public DependencyPersister(ResourceCache resourceCache, DependencyCache dependencyCache, @Nullable DatabaseSession session) {
    this.resourceCache = resourceCache;
    this.dependencyCache = dependencyCache;
    this.session = session;
  }

  public DependencyPersister(ResourceCache resourceCache, DependencyCache dependencyCache) {
    this(resourceCache, dependencyCache, null);
  }

  public void saveDependency(Project project, Dependency dependency) {
    BatchResource fromResource = resourceCache.get(dependency.getFrom());
    BatchResource toResource = resourceCache.get(dependency.getTo());
    BatchResource projectResource = resourceCache.get(project);

    if (fromResource.isFile() && toResource.isFile()) {
      dependencyCache.put(project.getEffectiveKey(), new DefaultDependency().setFromKey(fromResource.key()).setToKey(toResource.key()).setWeight(dependency.getWeight()));
    }

    if (session != null) {
      saveInDB(project, dependency, fromResource, toResource, projectResource);
    }
  }

  private void saveInDB(Project project, Dependency dependency, BatchResource fromResource, BatchResource toResource, BatchResource projectResource) {
    DependencyDto model = new DependencyDto();
    model.setProjectSnapshotId(projectResource.snapshotId());
    model.setUsage(dependency.getUsage());
    model.setWeight(dependency.getWeight());

    model.setFromComponentUuid(fromResource.resource().getUuid());
    model.setFromScope(fromResource.resource().getScope());
    model.setFromSnapshotId(fromResource.snapshotId());

    model.setToComponentUuid(toResource.resource().getUuid());
    model.setToScope(toResource.resource().getScope());
    model.setToSnapshotId(toResource.snapshotId());

    Dependency parentDependency = dependency.getParent();
    if (parentDependency != null) {
      if (parentDependency.getId() == null) {
        saveDependency(project, parentDependency);
      }
      model.setParentDependencyId(parentDependency.getId());
    }
    session.save(model);
    dependency.setId(model.getId());
  }
}

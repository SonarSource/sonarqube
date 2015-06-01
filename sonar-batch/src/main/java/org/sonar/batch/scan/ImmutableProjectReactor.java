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
package org.sonar.batch.scan;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

/**
 * Immutable copy of project reactor after all modifications have been applied (see {@link ImmutableProjectReactorProvider}).
 */
@BatchSide
public class ImmutableProjectReactor {

  private ProjectDefinition root;
  private Map<String, ProjectDefinition> byKey = new HashMap<>();

  public ImmutableProjectReactor(ProjectDefinition root) {
    if (root.getParent() != null) {
      throw new IllegalArgumentException("Not a root project: " + root);
    }
    this.root = root;
    collectProjects(root);
  }

  public Collection<ProjectDefinition> getProjects() {
    return byKey.values();
  }

  /**
   * Populates list of projects from hierarchy.
   */
  private void collectProjects(ProjectDefinition def) {
    if (byKey.containsKey(def.getKeyWithBranch())) {
      throw new IllegalStateException("Duplicate module key in reactor: " + def.getKeyWithBranch());
    }
    byKey.put(def.getKeyWithBranch(), def);
    for (ProjectDefinition child : def.getSubProjects()) {
      collectProjects(child);
    }
  }

  public ProjectDefinition getRoot() {
    return root;
  }

  @CheckForNull
  public ProjectDefinition getProjectDefinition(String keyWithBranch) {
    return byKey.get(keyWithBranch);
  }
}

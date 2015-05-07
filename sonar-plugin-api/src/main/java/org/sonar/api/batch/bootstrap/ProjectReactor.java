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
package org.sonar.api.batch.bootstrap;

import org.sonar.api.BatchSide;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.9
 */
@BatchSide
public class ProjectReactor {

  private ProjectDefinition root;

  public ProjectReactor(ProjectDefinition root) {
    if (root.getParent() != null) {
      throw new IllegalArgumentException("Not a root project: " + root);
    }
    this.root = root;
  }

  public List<ProjectDefinition> getProjects() {
    return collectProjects(root, new ArrayList<ProjectDefinition>());
  }

  /**
   * Populates list of projects from hierarchy.
   */
  private static List<ProjectDefinition> collectProjects(ProjectDefinition def, List<ProjectDefinition> collected) {
    collected.add(def);
    for (ProjectDefinition child : def.getSubProjects()) {
      collectProjects(child, collected);
    }
    return collected;
  }

  public ProjectDefinition getRoot() {
    return root;
  }

  public ProjectDefinition getProject(String key) {
    for (ProjectDefinition p : getProjects()) {
      if (key.equals(p.getKey())) {
        return p;
      }
    }
    return null;
  }
}

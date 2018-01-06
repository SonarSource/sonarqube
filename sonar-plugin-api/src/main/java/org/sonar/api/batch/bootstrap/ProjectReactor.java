/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.bootstrap;

import org.sonar.api.batch.ScannerSide;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable project definitions that can be modified by {@link ProjectBuilder} extensions.
 * 
 * @deprecated since 6.5 plugins should no longer modify the project's structure
 * @since 2.9
 */
@Deprecated
@ScannerSide
public class ProjectReactor implements ProjectKey {

  private ProjectDefinition root;

  public ProjectReactor(ProjectDefinition root) {
    if (root.getParent() != null) {
      throw new IllegalArgumentException("Not a root project: " + root);
    }
    this.root = root;
  }

  public List<ProjectDefinition> getProjects() {
    return collectProjects(root, new ArrayList<>());
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

  @Override
  public String get() {
    if (root != null) {
      return root.getKeyWithBranch();
    }
    return null;
  }
}

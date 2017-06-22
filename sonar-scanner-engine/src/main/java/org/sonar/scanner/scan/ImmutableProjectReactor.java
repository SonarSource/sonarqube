/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;

/**
 * Immutable copy of project reactor after all modifications have been applied (see {@link ImmutableProjectReactorProvider}).
 */
@Immutable
@ScannerSide
public class ImmutableProjectReactor {

  private final ImmutableProjectDefinition root;
  private final Map<String, ImmutableProjectDefinition> byKey;

  public ImmutableProjectReactor(ImmutableProjectDefinition root) {
    if (root.getParent() != null) {
      throw new IllegalArgumentException("Not a root project: " + root);
    }
    this.root = root;
    Map<String, ImmutableProjectDefinition> map = new LinkedHashMap<>();
    collectProjects(root, map);
    this.byKey = Collections.unmodifiableMap(map);
  }

  public Collection<ImmutableProjectDefinition> getProjects() {
    return byKey.values();
  }

  /**
   * Populates list of projects from hierarchy.
   */
  private void collectProjects(ImmutableProjectDefinition def, Map<String, ImmutableProjectDefinition> map) {
    if (map.put(def.getKeyWithBranch(), def) != null) {
      throw new IllegalStateException("Duplicate module key in reactor: " + def.getKeyWithBranch());
    }
    for (ImmutableProjectDefinition child : def.getSubProjects()) {
      collectProjects(child, map);
    }
  }

  public ImmutableProjectDefinition getRoot() {
    return root;
  }

  @CheckForNull
  public ImmutableProjectDefinition getProjectDefinition(String keyWithBranch) {
    return byKey.get(keyWithBranch);
  }
}

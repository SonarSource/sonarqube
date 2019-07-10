/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.scanner.fs.InputModuleHierarchy;

@Immutable
public class DefaultInputModuleHierarchy implements InputModuleHierarchy {
  private final DefaultInputModule root;
  private final Map<DefaultInputModule, DefaultInputModule> parents;
  private final Map<DefaultInputModule, List<DefaultInputModule>> children;

  public DefaultInputModuleHierarchy(DefaultInputModule root) {
    this.children = Collections.emptyMap();
    this.parents = Collections.emptyMap();
    this.root = root;
  }

  /**
   * Map of child->parent. Neither the Keys or values can be null.
   */
  public DefaultInputModuleHierarchy(DefaultInputModule root, Map<DefaultInputModule, DefaultInputModule> parents) {
    Map<DefaultInputModule, List<DefaultInputModule>> childrenBuilder = new HashMap<>();

    for (Map.Entry<DefaultInputModule, DefaultInputModule> e : parents.entrySet()) {
      childrenBuilder.computeIfAbsent(e.getValue(), x -> new ArrayList<>()).add(e.getKey());
    }

    this.children = Collections.unmodifiableMap(childrenBuilder);
    this.parents = Collections.unmodifiableMap(new HashMap<>(parents));
    this.root = root;
  }

  @Override
  public DefaultInputModule root() {
    return root;
  }

  @Override
  public Collection<DefaultInputModule> children(DefaultInputModule component) {
    return children.getOrDefault(component, Collections.emptyList());
  }

  @Override
  public DefaultInputModule parent(DefaultInputModule component) {
    return parents.get(component);
  }

  @Override
  public boolean isRoot(DefaultInputModule module) {
    return root.equals(module);
  }

  @Override
  @CheckForNull
  public String relativePath(DefaultInputModule module) {
    AbstractProjectOrModule parent = parent(module);
    if (parent == null) {
      return "";
    }
    Path parentBaseDir = parent.getBaseDir();
    Path moduleBaseDir = module.getBaseDir();

    return PathResolver.relativize(parentBaseDir, moduleBaseDir).orElse(null);
  }

  public String relativePathToRoot(DefaultInputModule module) {
    Path rootBaseDir = root.getBaseDir();
    Path moduleBaseDir = module.getBaseDir();

    return PathResolver.relativize(rootBaseDir, moduleBaseDir).orElse(null);
  }
}

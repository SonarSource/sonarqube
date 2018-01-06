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
package org.sonar.scanner.scan;

import com.google.common.collect.ImmutableMultimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.scan.filesystem.PathResolver;

@Immutable
public class DefaultInputModuleHierarchy implements InputModuleHierarchy {
  private final DefaultInputModule root;
  private final Map<DefaultInputModule, DefaultInputModule> parents;
  private final ImmutableMultimap<DefaultInputModule, DefaultInputModule> children;

  public DefaultInputModuleHierarchy(DefaultInputModule parent, DefaultInputModule child) {
    this(Collections.singletonMap(child, parent));
  }

  public DefaultInputModuleHierarchy(DefaultInputModule root) {
    this.children = new ImmutableMultimap.Builder<DefaultInputModule, DefaultInputModule>().build();
    this.parents = Collections.emptyMap();
    this.root = root;
  }

  /**
   * Map of child->parent. Neither the Keys or values can be null.
   */
  public DefaultInputModuleHierarchy(Map<DefaultInputModule, DefaultInputModule> parents) {
    ImmutableMultimap.Builder<DefaultInputModule, DefaultInputModule> childrenBuilder = new ImmutableMultimap.Builder<>();

    for (Map.Entry<DefaultInputModule, DefaultInputModule> e : parents.entrySet()) {
      childrenBuilder.put(e.getValue(), e.getKey());
    }

    this.children = childrenBuilder.build();
    this.parents = Collections.unmodifiableMap(new HashMap<>(parents));
    this.root = findRoot(parents);
  }

  private static DefaultInputModule findRoot(Map<DefaultInputModule, DefaultInputModule> parents) {
    DefaultInputModule r = null;
    for (DefaultInputModule parent : parents.values()) {
      if (!parents.containsKey(parent)) {
        if (r != null && r != parent) {
          throw new IllegalStateException(String.format("Found two modules without parent: '%s' and '%s'", r.key(), parent.key()));
        }
        r = parent;
      }
    }
    if (r == null) {
      throw new IllegalStateException("Found no root module");
    }
    return r;
  }

  @Override
  public DefaultInputModule root() {
    return root;
  }

  @Override
  public Collection<DefaultInputModule> children(InputModule component) {
    return children.get((DefaultInputModule) component);
  }

  @Override
  public DefaultInputModule parent(InputModule component) {
    return parents.get(component);
  }

  @Override
  public boolean isRoot(InputModule module) {
    return root.equals(module);
  }

  @Override
  @CheckForNull
  public String relativePath(InputModule module) {
    DefaultInputModule parent = parent(module);
    if (parent == null) {
      return null;
    }
    DefaultInputModule inputModule = (DefaultInputModule) module;
    Path parentBaseDir = parent.getBaseDir();
    Path moduleBaseDir = inputModule.getBaseDir();

    return PathResolver.relativize(parentBaseDir, moduleBaseDir).orElse(null);
  }
}

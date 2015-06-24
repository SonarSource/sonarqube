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
package org.sonar.server.computation.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Holds the reference to the root of the {@link Component} tree for the current CE run.
 */
public class TreeRootHolderImpl implements MutableTreeRootHolder {

  private Component root;
  private Map<Integer, Component> componentsByRef = new HashMap<>();

  @Override
  public void setRoot(Component newRoot) {
    this.root = Objects.requireNonNull(newRoot);
    feedComponentsByRef();
  }

  @Override
  public Component getRoot() {
    checkRoot();
    return this.root;
  }

  @Override
  public Component getComponentByRef(int ref) {
    checkRoot();
    Component component = componentsByRef.get(ref);
    if (component == null) {
      throw new IllegalArgumentException(String.format("Component '%s' hasn't been found", ref));
    }
    return component;
  }

  private void checkRoot() {
    if (this.root == null) {
      throw new IllegalStateException("Root has not been created yet");
    }
  }

  private void feedComponentsByRef() {
    new DepthTraversalTypeAwareVisitor(Component.Type.FILE, POST_ORDER) {
      @Override
      public void visitAny(Component component) {
        componentsByRef.put(component.getRef(), component);
      }
    }.visit(root);
  }
}

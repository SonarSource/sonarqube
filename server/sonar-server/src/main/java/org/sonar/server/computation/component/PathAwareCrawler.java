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

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * A {@link ComponentCrawler} which provide access to a representation of the path from the root to the currently visited
 * Component. It also provides a way to have an object associated to each Component and access it and all of its
 * parent's.
 * As for {@link DepthTraversalTypeAwareCrawler}, this crawler supports max depth visit and ordering.
 */
public abstract class PathAwareCrawler<T> extends PathAwareVisitorAdapter<T> implements ComponentCrawler {

  private final DequeBasedPath<T> stack = new DequeBasedPath<>();

  public PathAwareCrawler(Component.Type maxDepth, ComponentVisitor.Order order, StackElementFactory<T> factory) {
    super(maxDepth, order, factory);
  }

  @Override
  public void visit(Component component) {
    if (component.getType().isDeeperThan(getMaxDepth())) {
      return;
    }

    stack.add(new PathElementImpl<>(component, createForComponent(component)));

    if (getOrder() == PRE_ORDER) {
      visitNode(component);
    }

    visitChildren(component);

    if (getOrder() == POST_ORDER) {
      visitNode(component);
    }

    stack.pop();
  }

  private void visitChildren(Component component) {
    for (Component child : component.getChildren()) {
      if (!child.getType().isDeeperThan(getMaxDepth())) {
        visit(child);
      }
    }
  }

  private void visitNode(Component component) {
    visitAny(component, stack);
    switch (component.getType()) {
      case PROJECT:
        visitProject(component, stack);
        break;
      case MODULE:
        visitModule(component, stack);
        break;
      case DIRECTORY:
        visitDirectory(component, stack);
        break;
      case FILE:
        visitFile(component, stack);
        break;
      default:
        visitUnknown(component, stack);
    }
  }

  private T createForComponent(Component component) {
    switch (component.getType()) {
      case PROJECT:
        return getFactory().createForProject(component);
      case MODULE:
        return getFactory().createForModule(component);
      case DIRECTORY:
        return getFactory().createForDirectory(component);
      case FILE:
        return getFactory().createForFile(component);
      default:
        return getFactory().createForUnknown(component);
    }
  }

}

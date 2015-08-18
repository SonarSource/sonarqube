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

import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * A {@link ComponentCrawler} which provide access to a representation of the path from the root to the currently visited
 * Component. It also provides a way to have an object associated to each Component and access it and all of its
 * parent's.
 * As for {@link DepthTraversalTypeAwareCrawler}, this crawler supports max depth visit and ordering.
 */
public final class PathAwareCrawler<T> implements ComponentCrawler {

  private final PathAwareVisitor<T> visitor;
  private final DequeBasedPath<T> stack = new DequeBasedPath<>();

  public PathAwareCrawler(PathAwareVisitor<T> visitor) {
    this.visitor = requireNonNull(visitor);
  }

  @Override
  public void visit(Component component) {
    if (!verifyDepth(component)) {
      return;
    }

    stack.add(new PathElementImpl<>(component, createForComponent(component)));

    if (this.visitor.getOrder() == PRE_ORDER) {
      visitNode(component);
    }

    visitChildren(component);

    if (this.visitor.getOrder() == POST_ORDER) {
      visitNode(component);
    }

    stack.pop();
  }

  private boolean verifyDepth(Component component) {
    CrawlerDepthLimit maxDepth = this.visitor.getMaxDepth();
    return maxDepth.isSameAs(component.getType()) || maxDepth.isDeeperThan(component.getType());
  }

  private void visitChildren(Component component) {
    for (Component child : component.getChildren()) {
      if (verifyDepth(component)) {
        visit(child);
      }
    }
  }

  private void visitNode(Component component) {
    this.visitor.visitAny(component, stack);
    switch (component.getType()) {
      case PROJECT:
        this.visitor.visitProject(component, stack);
        break;
      case MODULE:
        this.visitor.visitModule(component, stack);
        break;
      case DIRECTORY:
        this.visitor.visitDirectory(component, stack);
        break;
      case FILE:
        this.visitor.visitFile(component, stack);
        break;
      case VIEW:
        this.visitor.visitView(component, stack);
        break;
      case SUBVIEW:
        this.visitor.visitSubView(component, stack);
        break;
      case PROJECT_VIEW:
        this.visitor.visitProjectView(component, stack);
        break;
      default:
        this.visitor.visitUnknown(component, stack);
    }
  }

  private T createForComponent(Component component) {
    switch (component.getType()) {
      case PROJECT:
        return this.visitor.getFactory().createForProject(component);
      case MODULE:
        return this.visitor.getFactory().createForModule(component);
      case DIRECTORY:
        return this.visitor.getFactory().createForDirectory(component);
      case FILE:
        return this.visitor.getFactory().createForFile(component);
      case VIEW:
        return this.visitor.getFactory().createForView(component);
      case SUBVIEW:
        return this.visitor.getFactory().createForSubView(component);
      case PROJECT_VIEW:
        return this.visitor.getFactory().createForProjectView(component);
      default:
        return this.visitor.getFactory().createForUnknown(component);
    }
  }

}

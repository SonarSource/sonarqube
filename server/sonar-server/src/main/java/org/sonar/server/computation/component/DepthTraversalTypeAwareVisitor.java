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

/**
 * Implementation of {@link TypeAwareVisitor} that implements a depth traversal of a {@link Component} tree.
 * <p>It supports visiting traversal in either pre-order or post-order</p>
 * It supports a max depth for crawling (component strictly deeper than the specified type will be ignored).
 */
public abstract class DepthTraversalTypeAwareVisitor implements TypeAwareVisitor {
  private final Component.Type maxDepth;
  private final Order order;

  protected DepthTraversalTypeAwareVisitor(Component.Type maxDepth, Order order) {
    this.maxDepth = requireNonNull(maxDepth);
    this.order = requireNonNull(order);
  }

  @Override
  public void visit(Component component) {
    if (component.getType().isDeeperThan(maxDepth)) {
      return;
    }

    if (order == Order.PRE_ORDER) {
      visitNode(component);
    }

    visitChildren(component);

    if (order == Order.POST_ORDER) {
      visitNode(component);
    }
  }

  private void visitNode(Component component) {
    switch (component.getType()) {
      case PROJECT:
        visitProject(component);
        break;
      case MODULE:
        visitModule(component);
        break;
      case DIRECTORY:
        visitDirectory(component);
        break;
      case FILE:
        visitFile(component);
        break;
      default:
        visitUnknown(component);
    }
  }

  private void visitChildren(Component component) {
    if (component.getType() != maxDepth) {
      for (Component child : component.getChildren()) {
        visit(child);
      }
    }
  }

  @Override
  public void visitProject(Component project) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitModule(Component module) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitDirectory(Component directory) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitFile(Component file) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitUnknown(Component unknownComponent) {
    // empty implementation, meant to be override at will by subclasses
  }

  public enum Order {
    PRE_ORDER, POST_ORDER
  }
}

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

import java.util.Objects;

/**
 * Implementation of {@link TypeAwareVisitor} that implements a depth first crawling of the
 * {@link ComponentImpl} tree. It supports a max depth for crawling (component strictly deeper than the specified
 * type will be ignored).
 */
public abstract class ChildFirstTypeAwareVisitor implements TypeAwareVisitor {
  private final Component.Type maxDepth;

  protected ChildFirstTypeAwareVisitor(Component.Type maxDepth) {
    this.maxDepth = Objects.requireNonNull(maxDepth);
  }

  @Override
  public void visit(Component component) {
    if (component.getType().isDeeperThan(maxDepth)) {
      return;
    }

    if (component.getType() != maxDepth) {
      for (Component child : component.getChildren()) {
        visit(child);
      }
    }

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

  @Override
  public void visitProject(Component tree) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitModule(Component tree) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitDirectory(Component tree) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitFile(Component tree) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitUnknown(Component tree) {
    // empty implementation, meant to be override at will by subclasses
  }

}

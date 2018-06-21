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
package org.sonar.server.computation.task.projectanalysis.component;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ComponentCrawler} that implements a depth traversal of a {@link Component} tree.
 * <p>It supports visiting traversal in either pre-order or post-order</p>
 * It supports a max depth for crawling (component strictly deeper than the specified type will be ignored).
 */
public final class DepthTraversalTypeAwareCrawler implements ComponentCrawler {
  private final TypeAwareVisitor visitor;

  public DepthTraversalTypeAwareCrawler(TypeAwareVisitor visitor) {
    this.visitor = requireNonNull(visitor);
  }

  @Override
  public void visit(Component component) {
    try {
      visitImpl(component);
    } catch (RuntimeException e) {
      VisitException.rethrowOrWrap(e, "Visit of Component {key=%s,uuid=%s,type=%s} failed", component.getKey(), component.getUuid(), component.getType());
    }
  }

  private void visitImpl(Component component) {
    if (!verifyDepth(component)) {
      return;
    }

    if (this.visitor.getOrder() == ComponentVisitor.Order.PRE_ORDER) {
      visitNode(component);
    }

    visitChildren(component);

    if (this.visitor.getOrder() == ComponentVisitor.Order.POST_ORDER) {
      visitNode(component);
    }
  }

  private boolean verifyDepth(Component component) {
    CrawlerDepthLimit maxDepth = this.visitor.getMaxDepth();
    return maxDepth.isSameAs(component.getType()) || maxDepth.isDeeperThan(component.getType());
  }

  private void visitNode(Component component) {
    this.visitor.visitAny(component);
    switch (component.getType()) {
      case PROJECT:
        this.visitor.visitProject(component);
        break;
      case MODULE:
        this.visitor.visitModule(component);
        break;
      case DIRECTORY:
        this.visitor.visitDirectory(component);
        break;
      case FILE:
        this.visitor.visitFile(component);
        break;
      case VIEW:
        this.visitor.visitView(component);
        break;
      case SUBVIEW:
        this.visitor.visitSubView(component);
        break;
      case PROJECT_VIEW:
        this.visitor.visitProjectView(component);
        break;
      default:
        throw new IllegalArgumentException("Unsupported Component type " + component.getType());
    }
  }

  private void visitChildren(Component component) {
    for (Component child : component.getChildren()) {
      if (verifyDepth(child)) {
        visit(child);
      }
    }
  }

}

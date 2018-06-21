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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

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
    try {
      visitImpl(component);
    } catch (RuntimeException e) {
      VisitException.rethrowOrWrap(
        e,
        "Visit failed for Component {key=%s,type=%s} %s",
        component.getKey(), component.getType(), new ComponentPathPrinter<>(stack));
    }
  }

  private void visitImpl(Component component) {
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
        throw new IllegalArgumentException(format("Unsupported component type %s, no visitor method to call", component.getType()));
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
        throw new IllegalArgumentException(format("Unsupported component type %s, can not create stack object", component.getType()));
    }
  }

  /**
   * A simple object wrapping the currentPath allowing to compute the string representing the path only if
   * the VisitException is actually built (ie. method {@link ComponentPathPrinter#toString()} is called
   * by the internal {@link String#format(String, Object...)} of
   * {@link VisitException#rethrowOrWrap(RuntimeException, String, Object...)}.
   */
  @Immutable
  private static final class ComponentPathPrinter<T> {

    private static final Joiner PATH_ELEMENTS_JOINER = Joiner.on("->");

    private final DequeBasedPath<T> currentPath;

    private ComponentPathPrinter(DequeBasedPath<T> currentPath) {
      this.currentPath = currentPath;
    }

    @Override
    public String toString() {
      if (currentPath.isRoot()) {
        return "";
      }
      return " located " + toKeyPath(currentPath);
    }

    private static <T> String toKeyPath(Iterable<PathAwareVisitor.PathElement<T>> currentPath) {
      return PATH_ELEMENTS_JOINER.join(from(currentPath).transform(PathElementToComponentAsString.INSTANCE).skip(1));
    }

    private enum PathElementToComponentAsString implements Function<PathAwareVisitor.PathElement<?>, String> {
      INSTANCE;

      @Override
      @Nonnull
      public String apply(@Nonnull PathAwareVisitor.PathElement<?> input) {
        return format("%s(type=%s)", input.getComponent().getKey(), input.getComponent().getType());
      }
    }
  }

}

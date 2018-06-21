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
 * A adapter of the {@link PathAwareVisitor} to be able to visit only some component types
 */
public abstract class PathAwareVisitorAdapter<T> implements PathAwareVisitor<T> {
  private final CrawlerDepthLimit maxDepth;
  private final Order order;
  private final StackElementFactory<T> factory;

  public PathAwareVisitorAdapter(CrawlerDepthLimit maxDepth, Order order, StackElementFactory<T> factory) {
    this.maxDepth = requireNonNull(maxDepth);
    this.order = requireNonNull(order);
    this.factory = requireNonNull(factory, "Factory can not be null");
  }

  @Override
  public CrawlerDepthLimit getMaxDepth() {
    return maxDepth;
  }

  @Override
  public Order getOrder() {
    return order;
  }

  @Override
  public StackElementFactory<T> getFactory() {
    return factory;
  }

  @Override
  public void visitProject(Component project, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitModule(Component module, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitDirectory(Component directory, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitFile(Component file, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitView(Component view, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitSubView(Component subView, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitProjectView(Component projectView, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  @Override
  public void visitAny(Component component, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  /**
   * A Simple implementation which uses the same factory method for all types which can be implemented by subclasses:
   * {@link #createForAny(Component)}.
   */
  public abstract static class SimpleStackElementFactory<T> implements StackElementFactory<T> {

    public abstract T createForAny(Component component);

    @Override
    public T createForProject(Component project) {
      return createForAny(project);
    }

    @Override
    public T createForModule(Component module) {
      return createForAny(module);
    }

    @Override
    public T createForDirectory(Component directory) {
      return createForAny(directory);
    }

    @Override
    public T createForFile(Component file) {
      return createForAny(file);
    }

    @Override
    public T createForView(Component view) {
      return createForAny(view);
    }

    @Override
    public T createForSubView(Component subView) {
      return createForAny(subView);
    }

    @Override
    public T createForProjectView(Component projectView) {
      return createForAny(projectView);
    }

  }

}

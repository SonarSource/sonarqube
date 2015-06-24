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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * A ComponentVisitor which provide access to a representation of the path from the root to the currently visited
 * Component. It also provides a way to have an object associated to each Component and access it and all of its
 * parent's.
 * As for {@link DepthTraversalTypeAwareVisitor}, this visitor supports max depth visit and ordering.
 */
public abstract class PathAwareVisitor<T> implements ComponentVisitor {
  private final Component.Type maxDepth;
  private final Order order;
  private final StackElementFactory<T> factory;
  private final DequeBasedPath<T> stack = new DequeBasedPath<>();

  public PathAwareVisitor(Component.Type maxDepth, Order order, StackElementFactory<T> factory) {
    this.maxDepth = requireNonNull(maxDepth);
    this.order = requireNonNull(order);
    this.factory = requireNonNull(factory, "Factory can not be null");
  }

  @Override
  public void visit(Component component) {
    if (component.getType().isDeeperThan(maxDepth)) {
      return;
    }

    stack.add(new PathElementImpl<>(component, createForComponent(component)));

    if (order == PRE_ORDER) {
      visitNode(component);
    }

    visitChildren(component);

    if (order == POST_ORDER) {
      visitNode(component);
    }

    stack.pop();
  }

  private T createForComponent(Component component) {
    switch (component.getType()) {
      case PROJECT:
        return factory.createForProject(component);
      case MODULE:
        return factory.createForModule(component);
      case DIRECTORY:
        return factory.createForDirectory(component);
      case FILE:
        return factory.createForFile(component);
      default:
        return factory.createForUnknown(component);
    }
  }

  private void visitChildren(Component component) {
    if (component.getType() != maxDepth) {
      for (Component child : component.getChildren()) {
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

  protected void visitProject(Component project, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  protected void visitModule(Component module, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  protected void visitDirectory(Component directory, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  protected void visitFile(Component file, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  protected void visitUnknown(Component unknownComponent, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  protected void visitAny(Component component, Path<T> path) {
    // empty implementation, meant to be override at will by subclasses
  }

  public interface StackElementFactory<T> {
    T createForProject(Component project);

    T createForModule(Component module);

    T createForDirectory(Component directory);

    T createForFile(Component file);

    T createForUnknown(Component file);
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
    public T createForUnknown(Component file) {
      return createForAny(file);
    }
  }

  private static class DequeBasedPath<T> implements Path<T>, Iterable<PathElement<T>> {
    private final Deque<PathElement<T>> deque = new ArrayDeque<>();

    @Override
    public T current() {
      return deque.getFirst().getElement();
    }

    @Override
    public T parent() {
      Iterator<PathElement<T>> iterator = deque.iterator();
      if (iterator.hasNext()) {
        iterator.next();
        if (iterator.hasNext()) {
          return iterator.next().getElement();
        }
      }
      throw new NoSuchElementException("Path is either empty or has only one element. There is no parent");
    }

    @Override
    public boolean isRoot() {
      return deque.size() == 1;
    }

    @Override
    public T root() {
      return deque.getLast().getElement();
    }

    @Override
    public Iterator<PathElement<T>> iterator() {
      return deque.iterator();
    }

    @Override
    public Iterable<PathElement<T>> getCurrentPath() {
      return this;
    }

    public void add(PathElement<T> pathElement) {
      deque.addFirst(pathElement);
    }

    public PathElement<T> pop() {
      return deque.pop();
    }
  }

  public interface Path<T> {
    /**
     * The stacked element of the current Component.
     */
    T current();

    /**
     * Tells whether the current Component is the root of the tree.
     */
    boolean isRoot();

    /**
     * The stacked element of the parent of the current Component.
     *
     * @throws NoSuchElementException if the current Component is the root of the tree
     * @see #isRoot()
     */
    T parent();

    /**
     * The stacked element of the root of the tree.
     */
    T root();

    /**
     * The path to the current Component as an Iterable of {@link PathAwareVisitor.PathElement} which starts with
     * the {@link PathAwareVisitor.PathElement} of the current Component and ends with the
     * {@link PathAwareVisitor.PathElement} of the root of the tree.
     */
    Iterable<PathElement<T>> getCurrentPath();
  }

  public interface PathElement<T> {
    /**
     * The Component on the path.
     */
    Component getComponent();

    /**
     * The stacked element for the Component of this PathElement.
     */
    T getElement();
  }

  private static final class PathElementImpl<T> implements PathElement<T> {
    private final Component component;
    private final T element;

    private PathElementImpl(Component component, T element) {
      this.component = component;
      this.element = element;
    }

    @Override
    public Component getComponent() {
      return component;
    }

    @Override
    public T getElement() {
      return element;
    }
  }
}

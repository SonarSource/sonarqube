/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

public class TypeAwareVisitorWrapper implements VisitorWrapper {

  private final TypeAwareVisitor delegate;

  public TypeAwareVisitorWrapper(TypeAwareVisitor delegate) {
    this.delegate = delegate;
  }

  @Override
  public ComponentVisitor getWrappedVisitor() {
    return this.delegate;
  }

  @Override
  public void beforeComponent(Component component) {
    // Nothing to do
  }

  @Override
  public void afterComponent(Component component) {
    // Nothing to do
  }

  @Override
  public void visitProject(Component tree) {
    delegate.visitProject(tree);
  }

  @Override
  public void visitDirectory(Component tree) {
    delegate.visitDirectory(tree);
  }

  @Override
  public void visitFile(Component tree) {
    delegate.visitFile(tree);
  }

  @Override
  public void visitView(Component view) {
    delegate.visitView(view);
  }

  @Override
  public void visitSubView(Component subView) {
    delegate.visitSubView(subView);
  }

  @Override
  public void visitProjectView(Component projectView) {
    delegate.visitProjectView(projectView);
  }

  @Override
  public void visitAny(Component component) {
    delegate.visitAny(component);
  }

  @Override
  public ComponentVisitor.Order getOrder() {
    return delegate.getOrder();
  }

  @Override
  public CrawlerDepthLimit getMaxDepth() {
    return delegate.getMaxDepth();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}

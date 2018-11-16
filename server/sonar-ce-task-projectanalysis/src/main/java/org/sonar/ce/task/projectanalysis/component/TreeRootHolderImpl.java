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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Holds the reference to the root of the {@link Component} tree for the current CE run.
 */
public class TreeRootHolderImpl implements MutableTreeRootHolder {
  @CheckForNull
  private Map<Integer, Component> componentsByRef;

  @CheckForNull
  private Map<Integer, Component> extendedComponentsByRef;

  private int size;
  private Component root;
  private Component extendedTreeRoot;

  @Override
  public boolean isEmpty() {
    return this.root == null;
  }

  @Override
  public MutableTreeRootHolder setRoots(Component root, Component reportRoot) {
    checkState(this.root == null, "root can not be set twice in holder");
    this.root = requireNonNull(root, "root can not be null");
    this.extendedTreeRoot = requireNonNull(reportRoot, "extended tree root can not be null");
    return this;
  }

  @Override
  public Component getRoot() {
    checkInitialized();
    return this.root;
  }

  @Override
  public Component getReportTreeRoot() {
    checkInitialized();
    return this.extendedTreeRoot;
  }

  @Override
  public Component getComponentByRef(int ref) {
    return getOptionalComponentByRef(ref)
      .orElseThrow(() -> new IllegalArgumentException(String.format("Component with ref '%s' can't be found", ref)));
  }

  @Override
  public Optional<Component> getOptionalComponentByRef(int ref) {
    checkInitialized();
    ensureComponentByRefIsPopulated();
    return Optional.ofNullable(componentsByRef.get(ref));
  }

  @Override
  public Component getReportTreeComponentByRef(int ref) {
    checkInitialized();
    ensureExtendedComponentByRefIsPopulated();
    Component c = extendedComponentsByRef.get(ref);
    if (c == null) {
      throw new IllegalArgumentException(String.format("Component with ref '%s' can't be found", ref));
    }
    return c;
  }

  @Override
  public int getSize() {
    checkInitialized();
    ensureComponentByRefIsPopulated();
    return size;
  }

  private void ensureExtendedComponentByRefIsPopulated() {
    if (extendedComponentsByRef != null) {
      return;
    }

    final ImmutableMap.Builder<Integer, Component> builder = ImmutableMap.builder();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          if (component.getReportAttributes().getRef() != null) {
            builder.put(component.getReportAttributes().getRef(), component);
          }
        }
      }).visit(this.extendedTreeRoot);
    this.extendedComponentsByRef = builder.build();
  }

  private void ensureComponentByRefIsPopulated() {
    if (componentsByRef != null) {
      return;
    }

    final ImmutableMap.Builder<Integer, Component> builder = ImmutableMap.builder();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          size++;
          if (component.getReportAttributes().getRef() != null) {
            builder.put(component.getReportAttributes().getRef(), component);
          }
        }
      }).visit(this.root);
    this.componentsByRef = builder.build();
  }

  private void checkInitialized() {
    checkState(this.root != null, "Holder has not been initialized yet");
  }
}

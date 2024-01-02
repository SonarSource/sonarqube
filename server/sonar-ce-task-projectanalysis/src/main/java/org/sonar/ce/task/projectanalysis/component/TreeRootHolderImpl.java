/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Holds the reference to the root of the {@link Component} tree for the current CE run.
 */
public class TreeRootHolderImpl implements MutableTreeRootHolder {
  @CheckForNull
  private Map<Integer, Component> componentsByRef = null;
  @CheckForNull
  private Map<Integer, Component> extendedComponentsByRef = null;
  @CheckForNull
  private Map<String, Component> componentsByUuid = null;
  private int size = 0;
  private Component root = null;
  private Component extendedTreeRoot = null;

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
  public Component getComponentByUuid(String uuid) {
    checkInitialized();
    ensureComponentByRefAndUuidArePopulated();
    return componentsByUuid.get(uuid);
  }

  @Override
  public Optional<Component> getOptionalComponentByRef(int ref) {
    checkInitialized();
    ensureComponentByRefAndUuidArePopulated();
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
    ensureComponentByRefAndUuidArePopulated();
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

  private void ensureComponentByRefAndUuidArePopulated() {
    if (componentsByRef != null && componentsByUuid != null) {
      return;
    }

    final ImmutableMap.Builder<Integer, Component> builderByRef = ImmutableMap.builder();
    final Map<String, Component> builderByUuid = new HashMap<>();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          size++;
          if (component.getReportAttributes().getRef() != null) {
            builderByRef.put(component.getReportAttributes().getRef(), component);
          }
          if (isNotBlank(component.getUuid())) {
            builderByUuid.put(component.getUuid(), component);
          }
        }
      }).visit(this.root);
    this.componentsByRef = builderByRef.build();
    this.componentsByUuid = ImmutableMap.copyOf(builderByUuid);
  }

  private void checkInitialized() {
    checkState(this.root != null, "Holder has not been initialized yet");
  }
}

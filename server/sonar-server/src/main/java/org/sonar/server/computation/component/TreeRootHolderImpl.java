/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Holds the reference to the root of the {@link Component} tree for the current CE run.
 */
public class TreeRootHolderImpl implements MutableTreeRootHolder {
  @CheckForNull
  private Map<Integer, Component> componentsByRef;
  @CheckForNull
  private Map<String, Component> componentsByKey;

  private Component root;

  @Override
  public MutableTreeRootHolder setRoot(Component root) {
    checkState(this.root == null, "root can not be set twice in holder");
    this.root = requireNonNull(root, "root can not be null");
    return this;
  }

  @Override
  public Component getRoot() {
    checkInitialized();
    return this.root;
  }

  @Override
  public Component getComponentByRef(int ref) {
    checkInitialized();
    ensureComponentByRefIsPopulated();
    Component component = componentsByRef.get(ref);
    checkArgument(component != null, "Component with ref '%s' can't be found", ref);
    return component;
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
          builder.put(component.getReportAttributes().getRef(), component);
        }
      }).visit(this.root);
    this.componentsByRef = builder.build();
  }

  @Override
  public Component getComponentByKey(String key) {
    checkKeyArgument(key);
    checkInitialized();
    ensureComponentByKeyIsPopulated();
    Component component = componentsByKey.get(key);
    checkArgument(component != null, "Component with key '%s' can't be found", key);
    return component;
  }

  @Override
  public boolean hasComponentWithKey(String key) {
    checkKeyArgument(key);
    checkInitialized();
    ensureComponentByKeyIsPopulated();

    return componentsByKey.containsKey(key);
  }

  private void checkInitialized() {
    checkState(this.root != null, "Holder has not been initialized yet");
  }

  private static void checkKeyArgument(String key) {
    requireNonNull(key, "key can not be null");
  }

  private void ensureComponentByKeyIsPopulated() {
    if (componentsByKey != null) {
      return;
    }

    final ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.LEAVES, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          builder.put(component.getKey(), component);
        }
      }).visit(this.root);
    this.componentsByKey = builder.build();
  }
}

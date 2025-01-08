/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class TreeRootHolderRule extends ExternalResource implements TreeRootHolder, AfterEachCallback {
  protected TreeRootHolderImpl delegate = new TreeRootHolderImpl();

  @CheckForNull
  private Map<String, Component> componentsByKey;

  @Override
  protected void after() {
    this.delegate = null;
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    after();
  }

  public TreeRootHolderRule setRoot(Component root) {
    return setRoots(root, root);
  }

  public TreeRootHolderRule setRoots(Component root, Component reportRoot) {
    delegate = new TreeRootHolderImpl();
    delegate.setRoots(root, reportRoot);
    return this;
  }

  public Component getComponentByKey(String key) {
    checkKeyArgument(key);
    ensureComponentByKeyIsPopulated();
    Component component = componentsByKey.get(key);
    checkArgument(component != null, "Component with key '%s' can't be found", key);
    return component;
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
      }).visit(getRoot());
    this.componentsByKey = builder.build();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Component getRoot() {
    return delegate.getRoot();
  }

  @Override
  public Component getReportTreeRoot() {
    return delegate.getReportTreeRoot();
  }

  @Override
  public Component getComponentByRef(int ref) {
    return delegate.getComponentByRef(ref);
  }

  @Override
  public Component getComponentByUuid(String uuid) {
    return delegate.getComponentByUuid(uuid);
  }

  @Override
  public Optional<Component> getOptionalComponentByRef(int ref) {
    return delegate.getOptionalComponentByRef(ref);
  }

  @Override public Component getReportTreeComponentByRef(int ref) {
    return delegate.getReportTreeComponentByRef(ref);
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

}

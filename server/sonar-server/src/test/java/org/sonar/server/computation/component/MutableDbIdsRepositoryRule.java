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

import com.google.common.base.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.server.computation.batch.TreeRootHolderRule;

import static com.google.common.base.Preconditions.checkState;

/**
 * Implementation of {@link DbIdsRepository} as a JUnit {@link org.junit.Rule} which supports both
 * {@link ViewsComponent} and {@link ReportComponent}.
 */
public class MutableDbIdsRepositoryRule extends ExternalResource implements MutableDbIdsRepository {
  @CheckForNull
  private final ComponentProvider componentProvider;
  private boolean providerInitialized = false;
  private MutableDbIdsRepository delegate = newDelegate();

  private MutableDbIdsRepositoryRule(@Nullable ComponentProvider componentProvider) {
    this.componentProvider = componentProvider;
  }

  public static MutableDbIdsRepositoryRule standalone() {
    return new MutableDbIdsRepositoryRule(null);
  }

  public static MutableDbIdsRepositoryRule create(TreeRootHolderRule treeRootHolder) {
    return new MutableDbIdsRepositoryRule(new TreeRootHolderComponentProvider(treeRootHolder));
  }

  public static MutableDbIdsRepositoryRule create(Component root) {
    return new MutableDbIdsRepositoryRule(new TreeComponentProvider(root));
  }

  private static MapBasedDbIdsRepository<String> newDelegate() {
    return new MapBasedDbIdsRepository<>(new Function<Component, String>() {
      @Nullable
      @Override
      public String apply(Component input) {
        return input.getType().isReportType() ? String.valueOf(input.getReportAttributes().getRef()) : input.getKey();
      }
    });
  }

  @Override
  protected void before() throws Throwable {
    this.delegate = newDelegate();
    if (this.componentProvider != null) {
      this.providerInitialized = false;
    }
  }

  public DbIdsRepository setComponentId(int componentRef, long componentId) {
    checkAndInitProvider();
    return delegate.setComponentId(componentProvider.getByRef(componentRef), componentId);
  }

  public DbIdsRepository setSnapshotId(int componentRef, long snapshotId) {
    checkAndInitProvider();
    return delegate.setSnapshotId(componentProvider.getByRef(componentRef), snapshotId);
  }

  private void checkAndInitProvider() {
    checkState(this.componentProvider != null, "Can not use methods set taking a ref if no ComponentProvider has been set");
    if (!this.providerInitialized) {
      this.componentProvider.init();
      this.providerInitialized = true;
    }
  }

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    return delegate.setComponentId(component, componentId);
  }

  @Override
  public DbIdsRepository setSnapshotId(Component component, long snapshotId) {
    return delegate.setSnapshotId(component, snapshotId);
  }

  @Override
  public long getComponentId(Component component) {
    return delegate.getComponentId(component);
  }

  @Override
  public long getSnapshotId(Component component) {
    return delegate.getSnapshotId(component);
  }
}

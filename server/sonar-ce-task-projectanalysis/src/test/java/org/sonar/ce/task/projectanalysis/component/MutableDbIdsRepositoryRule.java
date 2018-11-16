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

import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;

/**
 * Implementation of {@link DbIdsRepository} as a JUnit {@link org.junit.Rule} which supports both
 * {@link ViewsComponent} and {@link ReportComponent}.
 */
public class MutableDbIdsRepositoryRule extends ExternalResource implements MutableDbIdsRepository {
  private final ComponentProvider componentProvider;
  private MutableDbIdsRepository delegate = newDelegate();

  private MutableDbIdsRepositoryRule(ComponentProvider componentProvider) {
    this.componentProvider = componentProvider;
  }

  public static MutableDbIdsRepositoryRule standalone() {
    return new MutableDbIdsRepositoryRule(NoComponentProvider.INSTANCE);
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
        return input.getType().isReportType() ? input.getUuid() : input.getDbKey();
      }
    });
  }

  @Override
  protected void before() {
    this.delegate = newDelegate();
  }

  public DbIdsRepository setComponentId(int componentRef, long componentId) {
    this.componentProvider.ensureInitialized();
    return delegate.setComponentId(componentProvider.getByRef(componentRef), componentId);
  }

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    return delegate.setComponentId(component, componentId);
  }

  @Override
  public DbIdsRepository setDeveloperId(Developer developer, long developerId) {
    return delegate.setDeveloperId(developer, developerId);
  }

  @Override
  public long getDeveloperId(Developer developer) {
    return delegate.getDeveloperId(developer);
  }

  @Override
  public long getComponentId(Component component) {
    return delegate.getComponentId(component);
  }

}

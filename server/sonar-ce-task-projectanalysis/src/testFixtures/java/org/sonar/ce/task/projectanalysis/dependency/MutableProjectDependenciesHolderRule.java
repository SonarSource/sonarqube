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
package org.sonar.ce.task.projectanalysis.dependency;

import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

public class MutableProjectDependenciesHolderRule extends ExternalResource implements MutableProjectDependenciesHolder, AfterEachCallback {

  private ProjectDependenciesHolderImpl delegate = new ProjectDependenciesHolderImpl();

  @Override
  protected void after() {
    delegate = new ProjectDependenciesHolderImpl();
  }

  public MutableProjectDependenciesHolderRule setDependencies(List<ProjectDependency> dependencies) {
    delegate.setDependencies(dependencies);
    return this;
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public List<ProjectDependency> getDependencies() {
    return delegate.getDependencies();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    after();
  }

}

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

public final class TreeRootHolderComponentProvider extends AbstractComponentProvider {
  private final TreeRootHolder treeRootHolder;
  private TreeComponentProvider delegate;

  public TreeRootHolderComponentProvider(TreeRootHolder treeRootHolder) {
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  protected void ensureInitializedImpl() {
    if (this.delegate == null) {
      this.delegate = new TreeComponentProvider(treeRootHolder.getRoot());
      this.delegate.ensureInitialized();
    }
  }

  @Override
  protected void resetImpl() {
    this.delegate = null;
  }

  @Override
  protected Component getByRefImpl(int componentRef) {
    return delegate.getByRef(componentRef);
  }
}

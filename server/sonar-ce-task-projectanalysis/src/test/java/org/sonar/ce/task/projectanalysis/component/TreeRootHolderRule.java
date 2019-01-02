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

import java.util.Optional;
import org.junit.rules.ExternalResource;

public class TreeRootHolderRule extends ExternalResource implements TreeRootHolder {
  protected TreeRootHolderImpl delegate = new TreeRootHolderImpl();

  @Override
  protected void after() {
    this.delegate = null;
  }

  public TreeRootHolderRule setRoot(Component root) {
    return setRoots(root, root);
  }

  public TreeRootHolderRule setRoots(Component root, Component reportRoot) {
    delegate = new TreeRootHolderImpl();
    delegate.setRoots(root, reportRoot);
    return this;
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

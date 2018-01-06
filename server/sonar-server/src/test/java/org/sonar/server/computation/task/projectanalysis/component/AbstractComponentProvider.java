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

import static com.google.common.base.Preconditions.checkState;

abstract class AbstractComponentProvider implements ComponentProvider {
  private boolean initialized = false;

  @Override
  public void ensureInitialized() {
    if (!this.initialized) {
      ensureInitializedImpl();
      this.initialized = true;
    }
  }

  protected abstract void ensureInitializedImpl();

  @Override
  public void reset() {
    resetImpl();
    this.initialized = false;
  }

  protected abstract void resetImpl();

  @Override
  public Component getByRef(int componentRef) {
    checkState(this.initialized, "%s has not been initialized", getClass().getSimpleName());
    return getByRefImpl(componentRef);
  }

  protected abstract Component getByRefImpl(int componentRef);
}

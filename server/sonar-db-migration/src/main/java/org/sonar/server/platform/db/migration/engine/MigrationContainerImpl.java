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
package org.sonar.server.platform.db.migration.engine;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.StartableCloseableSafeLifecyleStrategy;

import static java.util.Objects.requireNonNull;

public class MigrationContainerImpl extends ComponentContainer implements MigrationContainer {

  public MigrationContainerImpl(ComponentContainer parent, MigrationContainerPopulator populator) {
    super(createContainer(requireNonNull(parent)), parent.getComponentByType(PropertyDefinitions.class));

    populateContainer(requireNonNull(populator));
    startComponents();
  }

  private void populateContainer(MigrationContainerPopulator populator) {
    populator.populateContainer(this);
  }

  /**
   * Creates a PicContainer which extends the specified ComponentContainer <strong>but is not referenced in return</strong>.
   */
  private static MutablePicoContainer createContainer(ComponentContainer parent) {
    LifecycleStrategy lifecycleStrategy = new StartableCloseableSafeLifecyleStrategy() {
      @Override
      public boolean isLazy(ComponentAdapter<?> adapter) {
        return true;
      }
    };
    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, parent.getPicoContainer(), new NullComponentMonitor());
  }

  @Override
  public void cleanup() {
    stopComponents();
  }

  @Override
  public String toString() {
    return "MigrationContainerImpl";
  }
}

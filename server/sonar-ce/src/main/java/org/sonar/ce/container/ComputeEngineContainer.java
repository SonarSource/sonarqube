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
package org.sonar.ce.container;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.Props;

public class ComputeEngineContainer {
  private final ComponentContainer componentContainer;

  public ComputeEngineContainer() {
    this.componentContainer = new ComponentContainer();
  }

  public ComputeEngineContainer configure(Props props) {
    this.componentContainer.add(
      props.rawProperties()
      );
    return this;
  }

  public ComputeEngineContainer start() {
    this.componentContainer.startComponents();
    return this;
  }

  public ComputeEngineContainer stop() {
    this.componentContainer.stopComponents();
    return this;
  }

  @VisibleForTesting
  protected ComponentContainer getComponentContainer() {
    return componentContainer;
  }
}

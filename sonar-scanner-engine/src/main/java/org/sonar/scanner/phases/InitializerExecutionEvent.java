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
package org.sonar.scanner.phases;

import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.events.InitializerExecutionHandler;

class InitializerExecutionEvent extends AbstractPhaseEvent<InitializerExecutionHandler>
  implements InitializerExecutionHandler.InitializerExecutionEvent {

  private final Initializer initializer;

  InitializerExecutionEvent(Initializer initializer, boolean start) {
    super(start);
    this.initializer = initializer;
  }

  @Override
  public Initializer getInitializer() {
    return initializer;
  }

  @Override
  public void dispatch(InitializerExecutionHandler handler) {
    handler.onInitializerExecution(this);
  }

  @Override
  public Class getType() {
    return InitializerExecutionHandler.class;
  }

}

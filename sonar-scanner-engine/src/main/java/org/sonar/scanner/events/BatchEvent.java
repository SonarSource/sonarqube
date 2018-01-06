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
package org.sonar.scanner.events;

import org.sonar.api.batch.events.EventHandler;

/**
 * Root of all Sonar Batch events.
 * 
 * @param <H> handler type
 */
public abstract class BatchEvent<H extends EventHandler> {

  protected BatchEvent() {
  }

  /**
   * Do not call directly - should be called only by {@link EventBus}.
   * Typically should be implemented as following: <code>handler.onEvent(this)</code>
   */
  protected abstract void dispatch(H handler);

  /**
   * Returns class of associated handler. Used by {@link EventBus} to dispatch events to the correct handlers.
   */
  protected abstract Class getType();

}

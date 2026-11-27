/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.application;

import java.util.function.Consumer;
import org.sonar.process.ProcessId;

/**
 * Called when a process becomes operational. In cluster mode the event may come from another node.
 * Subscribe through {@link AppState#addListener(AppStateListener)}. The process id is passed to {@link #accept}.
 */
@FunctionalInterface
public interface AppStateListener extends Consumer<ProcessId> {

  /**
   * Called on this node when another cluster member leaves.
   * Nodes that are still starting re-evaluate leadership, so a web leader that
   * disappears before becoming operational does not leave the others waiting.
   */
  default void onClusterMemberLeft() {
    // Listeners that only react to operational processes ignore membership changes.
  }
}

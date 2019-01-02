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
package org.sonar.application;

import java.util.Optional;
import org.sonar.process.ProcessId;

public interface AppState extends AutoCloseable {

  void addListener(AppStateListener listener);

  /**
   * Whether the process with the specified {@code processId}
   * has been marked as operational.
   *
   * If parameter {@code local} is {@code true}, then only the
   * process on the local node is requested.
   *
   * If parameter {@code local} is {@code false}, then only
   * the processes on remote nodes are requested, excluding
   * the local node. In this case at least one process must
   * be marked as operational.
   */
  boolean isOperational(ProcessId processId, boolean local);

  /**
   * Mark local process as operational. In cluster mode, this
   * event is propagated to all nodes.
   */
  void setOperational(ProcessId processId);

  boolean tryToLockWebLeader();

  void reset();

  void registerSonarQubeVersion(String sonarqubeVersion);

  void registerClusterName(String clusterName);

  Optional<String> getLeaderHostName();

  @Override
  void close();
}

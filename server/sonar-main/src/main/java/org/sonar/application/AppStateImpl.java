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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;

public class AppStateImpl implements AppState {

  private final Map<ProcessId, Boolean> processes = new EnumMap<>(ProcessId.class);
  private final List<AppStateListener> listeners = new ArrayList<>();
  private final AtomicBoolean webLeaderLocked = new AtomicBoolean(false);

  @Override
  public void addListener(AppStateListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public boolean isOperational(ProcessId processId, boolean local) {
    return processes.computeIfAbsent(processId, p -> false);
  }

  @Override
  public void setOperational(ProcessId processId) {
    processes.put(processId, true);
    listeners.forEach(l -> l.onAppStateOperational(processId));
  }

  @Override
  public boolean tryToLockWebLeader() {
    return webLeaderLocked.compareAndSet(false, true);
  }

  @Override
  public void reset() {
    webLeaderLocked.set(false);
    processes.clear();
  }

  @Override
  public void registerSonarQubeVersion(String sonarqubeVersion) {
    // Nothing to do on non clustered version
  }

  @Override
  public void registerClusterName(String clusterName) {
    // Nothing to do on non clustered version
  }

  @Override
  public Optional<String> getLeaderHostName() {
    return Optional.of(NetworkUtilsImpl.INSTANCE.getHostname());
  }

  @Override
  public void close() {
    // nothing to do
  }
}

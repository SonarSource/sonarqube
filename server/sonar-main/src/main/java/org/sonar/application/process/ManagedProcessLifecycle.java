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
package org.sonar.application.process;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessId;

import static org.sonar.application.process.ManagedProcessLifecycle.State.FINALIZE_STOPPING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.HARD_STOPPING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.INIT;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STARTED;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STARTING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STOPPED;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STOPPING;

public class ManagedProcessLifecycle {

  public enum State {
    INIT, STARTING, STARTED, STOPPING, HARD_STOPPING, FINALIZE_STOPPING, STOPPED
  }

  private static final Logger LOG = LoggerFactory.getLogger(ManagedProcessLifecycle.class);
  private static final Map<State, Set<State>> TRANSITIONS = buildTransitions();

  private final ProcessId processId;
  private final List<ProcessLifecycleListener> listeners;
  private State state;

  public ManagedProcessLifecycle(ProcessId processId, List<ProcessLifecycleListener> listeners) {
    this.processId = processId;
    this.listeners = listeners;
    this.state = INIT;
  }

  private static Map<State, Set<State>> buildTransitions() {
    Map<State, Set<State>> res = new EnumMap<>(State.class);
    res.put(INIT, toSet(STARTING));
    res.put(STARTING, toSet(STARTED, STOPPING, HARD_STOPPING));
    res.put(STARTED, toSet(STOPPING, HARD_STOPPING));
    res.put(STOPPING, toSet(HARD_STOPPING, FINALIZE_STOPPING));
    res.put(HARD_STOPPING, toSet(FINALIZE_STOPPING));
    res.put(FINALIZE_STOPPING, toSet(STOPPED));
    res.put(STOPPED, toSet());
    return Collections.unmodifiableMap(res);
  }

  private static Set<State> toSet(State... states) {
    if (states.length == 0) {
      return Collections.emptySet();
    }
    if (states.length == 1) {
      return Collections.singleton(states[0]);
    }
    return EnumSet.copyOf(Arrays.asList(states));
  }

  State getState() {
    return state;
  }

  synchronized boolean tryToMoveTo(State to) {
    boolean res = false;
    State currentState = state;
    if (TRANSITIONS.get(currentState).contains(to)) {
      this.state = to;
      res = true;
      listeners.forEach(listener -> listener.onProcessState(processId, to));
    }
    LOG.debug("{} tryToMoveTo {} from {} to {} => {}", Thread.currentThread().getName(), processId.getKey(), currentState, to, res);
    return res;
  }
}

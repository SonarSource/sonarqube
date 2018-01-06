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
package org.sonar.application;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.application.NodeLifecycle.State.INIT;
import static org.sonar.application.NodeLifecycle.State.OPERATIONAL;
import static org.sonar.application.NodeLifecycle.State.STARTING;
import static org.sonar.application.NodeLifecycle.State.STOPPED;
import static org.sonar.application.NodeLifecycle.State.STOPPING;

/**
 * Lifecycle of the cluster node, consolidating the states
 * of child processes.
 */
class NodeLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(NodeLifecycle.class);

  enum State {
    // initial state, does nothing
    INIT,

    // at least one process is still starting
    STARTING,

    // all the processes are started and operational
    OPERATIONAL,

    // at least one process is still stopping
    STOPPING,

    // all processes are stopped
    STOPPED
  }

  private static final Map<State, Set<State>> TRANSITIONS = buildTransitions();

  private State state = INIT;

  private static Map<State, Set<State>> buildTransitions() {
    Map<State, Set<State>> res = new EnumMap<>(State.class);
    res.put(INIT, toSet(STARTING));
    res.put(STARTING, toSet(OPERATIONAL, STOPPING, STOPPED));
    res.put(OPERATIONAL, toSet(STOPPING, STOPPED));
    res.put(STOPPING, toSet(STOPPED));
    res.put(STOPPED, toSet(STARTING));
    return res;
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
    }
    LOG.trace("tryToMoveTo from {} to {} => {}", currentState, to, res);
    return res;
  }
}

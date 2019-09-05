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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptySet;
import static org.sonar.application.NodeLifecycle.State.FINALIZE_STOPPING;
import static org.sonar.application.NodeLifecycle.State.HARD_STOPPING;
import static org.sonar.application.NodeLifecycle.State.INIT;
import static org.sonar.application.NodeLifecycle.State.OPERATIONAL;
import static org.sonar.application.NodeLifecycle.State.RESTARTING;
import static org.sonar.application.NodeLifecycle.State.STARTING;
import static org.sonar.application.NodeLifecycle.State.STOPPED;
import static org.sonar.application.NodeLifecycle.State.STOPPING;

/**
 * ManagedProcessLifecycle of the cluster node, consolidating the states
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

    // at least one process is still stopping as part of a node restart
    RESTARTING,

    // at least one process is still stopping as part of a node graceful stop
    STOPPING,

    // at least one process is still stopping as part of a node hard stop
    HARD_STOPPING,

    // a hard stop or regular stop *not part of a restart* is being finalized (clean up and log)
    FINALIZE_STOPPING,

    // all processes are stopped
    STOPPED
  }

  private static final Map<State, Set<State>> TRANSITIONS = buildTransitions();

  private State state = INIT;

  private static Map<State, Set<State>> buildTransitions() {
    Map<State, Set<State>> res = new EnumMap<>(State.class);
    res.put(INIT, toSet(STARTING));
    res.put(STARTING, toSet(OPERATIONAL, RESTARTING, STOPPING, HARD_STOPPING));
    res.put(OPERATIONAL, toSet(RESTARTING, STOPPING, HARD_STOPPING));
    res.put(STOPPING, toSet(FINALIZE_STOPPING, HARD_STOPPING));
    res.put(RESTARTING, toSet(STARTING, HARD_STOPPING));
    res.put(HARD_STOPPING, toSet(FINALIZE_STOPPING));
    res.put(FINALIZE_STOPPING, toSet(STOPPED));
    res.put(STOPPED, emptySet());
    return Collections.unmodifiableMap(res);
  }

  private static Set<State> toSet(State... states) {
    if (states.length == 0) {
      return emptySet();
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
    LOG.debug("{} tryToMoveTo from {} to {} => {}", Thread.currentThread().getName(), currentState, to, res);
    return res;
  }
}

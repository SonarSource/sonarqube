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
package org.sonar.process;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.process.Lifecycle.State.HARD_STOPPING;
import static org.sonar.process.Lifecycle.State.INIT;
import static org.sonar.process.Lifecycle.State.OPERATIONAL;
import static org.sonar.process.Lifecycle.State.RESTARTING;
import static org.sonar.process.Lifecycle.State.STARTED;
import static org.sonar.process.Lifecycle.State.STARTING;
import static org.sonar.process.Lifecycle.State.STOPPED;
import static org.sonar.process.Lifecycle.State.STOPPING;

public class Lifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(Lifecycle.class);

  public enum State {
    INIT, STARTING, STARTED, OPERATIONAL, RESTARTING, STOPPING, HARD_STOPPING, STOPPED
  }

  private static final Map<State, Set<State>> TRANSITIONS = buildTransitions();

  private AtomicReference<State> state = new AtomicReference<>(INIT);

  private static Map<State, Set<State>> buildTransitions() {
    Map<State, Set<State>> res = new EnumMap<>(State.class);
    res.put(INIT, toSet(STARTING));
    res.put(STARTING, toSet(STARTED, STOPPING, HARD_STOPPING));
    res.put(STARTED, toSet(OPERATIONAL, RESTARTING, STOPPING, HARD_STOPPING));
    res.put(OPERATIONAL, toSet(RESTARTING, STOPPING, HARD_STOPPING));
    res.put(RESTARTING, toSet(STARTING, HARD_STOPPING));
    res.put(STOPPING, toSet(HARD_STOPPING, STOPPED));
    res.put(HARD_STOPPING, toSet(STOPPED));
    res.put(STOPPED, toSet());
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

  public boolean isCurrentState(State candidateState) {
    State currentState = this.state.get();
    boolean res = currentState == candidateState;
    LOG.trace("isCurrentState({}): {} ({})", candidateState, res, currentState);
    return res;
  }

  public boolean tryToMoveTo(State to) {
    AtomicReference<State> lastFrom = new AtomicReference<>();
    State newState = this.state.updateAndGet(from -> {
      lastFrom.set(from);
      if (TRANSITIONS.get(from).contains(to)) {
        return to;
      }
      return from;
    });

    boolean updated = newState == to && lastFrom.get() != to;
    LOG.trace("tryToMoveTo from {} to {} => {}", lastFrom.get(), to, updated);
    return updated;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Lifecycle lifecycle = (Lifecycle) o;
    return state.get() == lifecycle.state.get();
  }

  @Override
  public int hashCode() {
    return state.get().hashCode();
  }
}

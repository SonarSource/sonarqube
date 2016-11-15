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
package org.sonar.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.Lifecycle.State;
import static org.sonar.process.Lifecycle.State.INIT;
import static org.sonar.process.Lifecycle.State.RESTARTING;
import static org.sonar.process.Lifecycle.State.STARTED;
import static org.sonar.process.Lifecycle.State.STARTING;
import static org.sonar.process.Lifecycle.State.STOPPING;
import static org.sonar.process.Lifecycle.State.values;

public class LifecycleTest {

  @Test
  public void equals_and_hashcode() {
    Lifecycle init = new Lifecycle();
    assertThat(init.equals(init)).isTrue();
    assertThat(init.equals(new Lifecycle())).isTrue();
    assertThat(init.equals("INIT")).isFalse();
    assertThat(init.equals(null)).isFalse();
    assertThat(init.hashCode()).isEqualTo(new Lifecycle().hashCode());

    // different state
    Lifecycle stopping = new Lifecycle();
    stopping.tryToMoveTo(STARTING);
    assertThat(stopping).isNotEqualTo(init);
  }

  @Test
  public void try_to_move_does_not_support_jumping_states() {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    Lifecycle lifecycle = new Lifecycle(listener);
    assertThat(lifecycle.getState()).isEqualTo(INIT);
    assertThat(listener.getTransitions()).isEmpty();

    assertThat(lifecycle.tryToMoveTo(STARTED)).isFalse();
    assertThat(lifecycle.getState()).isEqualTo(INIT);
    assertThat(listener.getTransitions()).isEmpty();

    assertThat(lifecycle.tryToMoveTo(STARTING)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(STARTING);
    assertThat(listener.getTransitions()).containsOnly(new Transition(INIT, STARTING));
  }

  @Test
  public void no_state_can_not_move_to_itself() {
    for (State state : values()) {
      assertThat(newLifeCycle(state).tryToMoveTo(state)).isFalse();
    }
  }

  @Test
  public void can_move_to_STOPPING_from_STARTING_and_STARTED_only() {
    for (State state : values()) {
      TestLifeCycleListener listener = new TestLifeCycleListener();
      boolean tryToMoveTo = newLifeCycle(state, listener).tryToMoveTo(STOPPING);
      if (state == STARTING || state == STARTED) {
        assertThat(tryToMoveTo).describedAs("from state " + state).isTrue();
        assertThat(listener.getTransitions()).containsOnly(new Transition(state, STOPPING));
      } else {
        assertThat(tryToMoveTo).describedAs("from state " + state).isFalse();
        assertThat(listener.getTransitions()).isEmpty();
      }
    }
  }

  @Test
  public void can_move_to_STARTING_from_RESTARTING() {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    assertThat(newLifeCycle(RESTARTING, listener).tryToMoveTo(STARTING)).isTrue();
    assertThat(listener.getTransitions()).containsOnly(new Transition(RESTARTING, STARTING));
  }

  private static Lifecycle newLifeCycle(State state, TestLifeCycleListener... listeners) {
    switch (state) {
      case INIT:
        return new Lifecycle(listeners);
      case STARTING:
        return newLifeCycle(INIT, state, listeners);
      case STARTED:
        return newLifeCycle(STARTING, state, listeners);
      case RESTARTING:
        return newLifeCycle(STARTED, state, listeners);
      case STOPPING:
        return newLifeCycle(STARTED, state, listeners);
      case HARD_STOPPING:
        return newLifeCycle(STARTING, state, listeners);
      case STOPPED:
        return newLifeCycle(STOPPING, state, listeners);
      default:
        throw new IllegalArgumentException("Unsupported state " + state);
    }
  }

  private static Lifecycle newLifeCycle(State from, State to, TestLifeCycleListener... listeners) {
    Lifecycle lifecycle;
    lifecycle = newLifeCycle(from, listeners);
    assertThat(lifecycle.tryToMoveTo(to)).isTrue();
    Arrays.stream(listeners).forEach(TestLifeCycleListener::clear);
    return lifecycle;
  }

  private static final class TestLifeCycleListener implements Lifecycle.LifecycleListener {
    private final List<Transition> transitions = new ArrayList<>();

    @Override
    public void successfulTransition(State from, State to) {
      transitions.add(new Transition(from, to));
    }

    public List<Transition> getTransitions() {
      return transitions;
    }

    public void clear() {
      this.transitions.clear();
    }
  }

  private static final class Transition {
    private final State from;
    private final State to;

    private Transition(State from, State to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Transition that = (Transition) o;
      return from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, to);
    }

    @Override
    public String toString() {
      return "Transition{" + from + " => " + to + '}';
    }
  }
}

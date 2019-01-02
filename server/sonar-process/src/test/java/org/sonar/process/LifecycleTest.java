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

import java.util.Objects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.Lifecycle.State;
import static org.sonar.process.Lifecycle.State.INIT;
import static org.sonar.process.Lifecycle.State.OPERATIONAL;
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
    Lifecycle lifecycle = new Lifecycle();
    assertThat(lifecycle.getState()).isEqualTo(INIT);

    assertThat(lifecycle.tryToMoveTo(STARTED)).isFalse();
    assertThat(lifecycle.getState()).isEqualTo(INIT);

    assertThat(lifecycle.tryToMoveTo(STARTING)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(STARTING);
  }

  @Test
  public void no_state_can_not_move_to_itself() {
    for (State state : values()) {
      assertThat(newLifeCycle(state).tryToMoveTo(state)).isFalse();
    }
  }

  @Test
  public void can_move_to_STOPPING_from_STARTING_STARTED_OPERATIONAL_only() {
    for (State state : values()) {
      boolean tryToMoveTo = newLifeCycle(state).tryToMoveTo(STOPPING);
      if (state == STARTING || state == STARTED || state == OPERATIONAL) {
        assertThat(tryToMoveTo).describedAs("from state " + state).isTrue();
      } else {
        assertThat(tryToMoveTo).describedAs("from state " + state).isFalse();
      }
    }
  }

  @Test
  public void can_move_to_OPERATIONAL_from_STARTED_only() {
    for (State state : values()) {
      boolean tryToMoveTo = newLifeCycle(state).tryToMoveTo(OPERATIONAL);
      if (state == STARTED) {
        assertThat(tryToMoveTo).describedAs("from state " + state).isTrue();
      } else {
        assertThat(tryToMoveTo).describedAs("from state " + state).isFalse();
      }
    }
  }

  @Test
  public void can_move_to_STARTING_from_RESTARTING() {
    assertThat(newLifeCycle(RESTARTING).tryToMoveTo(STARTING)).isTrue();
  }

  private static Lifecycle newLifeCycle(State state) {
    switch (state) {
      case INIT:
        return new Lifecycle();
      case STARTING:
        return newLifeCycle(INIT, state);
      case STARTED:
        return newLifeCycle(STARTING, state);
      case OPERATIONAL:
        return newLifeCycle(STARTED, state);
      case RESTARTING:
        return newLifeCycle(OPERATIONAL, state);
      case STOPPING:
        return newLifeCycle(OPERATIONAL, state);
      case HARD_STOPPING:
        return newLifeCycle(STARTING, state);
      case STOPPED:
        return newLifeCycle(STOPPING, state);
      default:
        throw new IllegalArgumentException("Unsupported state " + state);
    }
  }

  private static Lifecycle newLifeCycle(State from, State to) {
    Lifecycle lifecycle;
    lifecycle = newLifeCycle(from);
    assertThat(lifecycle.tryToMoveTo(to)).isTrue();
    return lifecycle;
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

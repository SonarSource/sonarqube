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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.application.process.Lifecycle;
import org.sonar.application.process.ProcessLifecycleListener;
import org.sonar.process.ProcessId;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.application.process.Lifecycle.State.INIT;
import static org.sonar.application.process.Lifecycle.State.STARTED;
import static org.sonar.application.process.Lifecycle.State.STARTING;
import static org.sonar.application.process.Lifecycle.State.STOPPING;

public class LifecycleTest {

  @Test
  public void initial_state_is_INIT() {
    Lifecycle lifecycle = new Lifecycle(ProcessId.ELASTICSEARCH, Collections.emptyList());
    assertThat(lifecycle.getState()).isEqualTo(INIT);
  }

  @Test
  public void try_to_move_does_not_support_jumping_states() {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    Lifecycle lifecycle = new Lifecycle(ProcessId.ELASTICSEARCH, asList(listener));
    assertThat(lifecycle.getState()).isEqualTo(INIT);
    assertThat(listener.states).isEmpty();

    assertThat(lifecycle.tryToMoveTo(STARTED)).isFalse();
    assertThat(lifecycle.getState()).isEqualTo(INIT);
    assertThat(listener.states).isEmpty();

    assertThat(lifecycle.tryToMoveTo(STARTING)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(STARTING);
    assertThat(listener.states).containsOnly(STARTING);
  }

  @Test
  public void no_state_can_not_move_to_itself() {
    for (Lifecycle.State state : Lifecycle.State.values()) {
      assertThat(newLifeCycle(state).tryToMoveTo(state)).isFalse();
    }
  }

  @Test
  public void can_move_to_STOPPING_from_STARTING_STARTED_only() {
    for (Lifecycle.State state : Lifecycle.State.values()) {
      TestLifeCycleListener listener = new TestLifeCycleListener();
      boolean tryToMoveTo = newLifeCycle(state, listener).tryToMoveTo(STOPPING);
      if (state == STARTING || state == STARTED) {
        assertThat(tryToMoveTo).as("from state " + state).isTrue();
        assertThat(listener.states).containsOnly(STOPPING);
      } else {
        assertThat(tryToMoveTo).as("from state " + state).isFalse();
        assertThat(listener.states).isEmpty();
      }
    }
  }

  @Test
  public void can_move_to_STARTED_from_STARTING_only() {
    for (Lifecycle.State state : Lifecycle.State.values()) {
      TestLifeCycleListener listener = new TestLifeCycleListener();
      boolean tryToMoveTo = newLifeCycle(state, listener).tryToMoveTo(STARTED);
      if (state == STARTING) {
        assertThat(tryToMoveTo).as("from state " + state).isTrue();
        assertThat(listener.states).containsOnly(STARTED);
      } else {
        assertThat(tryToMoveTo).as("from state " + state).isFalse();
        assertThat(listener.states).isEmpty();
      }
    }
  }

  private static Lifecycle newLifeCycle(Lifecycle.State state, TestLifeCycleListener... listeners) {
    return new Lifecycle(ProcessId.ELASTICSEARCH, Arrays.asList(listeners), state);
  }

  private static final class TestLifeCycleListener implements ProcessLifecycleListener {
    private final List<Lifecycle.State> states = new ArrayList<>();

    @Override
    public void onProcessState(ProcessId processId, Lifecycle.State state) {
      this.states.add(state);
    }
  }
}

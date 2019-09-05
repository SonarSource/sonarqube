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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.process.ProcessId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.application.process.ManagedProcessLifecycle.State;
import static org.sonar.application.process.ManagedProcessLifecycle.State.FINALIZE_STOPPING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.HARD_STOPPING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.INIT;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STARTED;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STARTING;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STOPPED;
import static org.sonar.application.process.ManagedProcessLifecycle.State.STOPPING;

@RunWith(DataProviderRunner.class)
public class ManagedProcessLifecycleTest {

  @Test
  public void initial_state_is_INIT() {
    ManagedProcessLifecycle lifecycle = new ManagedProcessLifecycle(ProcessId.ELASTICSEARCH, emptyList());
    assertThat(lifecycle.getState()).isEqualTo(INIT);
  }

  @Test
  public void listeners_are_not_notified_of_INIT_state() {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    new ManagedProcessLifecycle(ProcessId.ELASTICSEARCH, Arrays.asList(listener));
    assertThat(listener.states).isEmpty();
  }

  @Test
  public void try_to_move_does_not_support_jumping_states() {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    ManagedProcessLifecycle lifecycle = new ManagedProcessLifecycle(ProcessId.ELASTICSEARCH, asList(listener));
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
  @UseDataProvider("allStates")
  public void no_state_can_not_move_to_itself(State state) {
    assertThat(newLifeCycle(state).tryToMoveTo(state)).isFalse();
  }

  @Test
  @UseDataProvider("allStates")
  public void can_move_to_HARD_STOPPING_from_STARTING_AND_STARTED_and_STOPPING_only(State state) {
    verifyCanMoveTo(t -> t == STARTING || t == STARTED || t == STOPPING,
      state, HARD_STOPPING);
  }

  @Test
  @UseDataProvider("allStates")
  public void can_move_to_STARTED_from_STARTING_only(State state) {
    verifyCanMoveTo(t -> t == STARTING, state, STARTED);
  }

  private void verifyCanMoveTo(Predicate<State> isAllowed, State from, State to) {
    TestLifeCycleListener listener = new TestLifeCycleListener();
    ManagedProcessLifecycle underTest = newLifeCycle(from, listener);
    boolean tryToMoveTo = underTest.tryToMoveTo(to);
    if (isAllowed.test(from)) {
      assertThat(tryToMoveTo).isTrue();
    } else {
      assertThat(tryToMoveTo).isFalse();
    }
  }

  @Test
  @UseDataProvider("allStates")
  public void cannot_move_to_any_state_from_STOPPED(State state) {
    assertThat(newLifeCycle(STOPPED).tryToMoveTo(state)).isFalse();
  }

  private static ManagedProcessLifecycle newLifeCycle(State from, State to, TestLifeCycleListener... listeners) {
    ManagedProcessLifecycle lifecycle = newLifeCycle(from, listeners);
    assertThat(lifecycle.tryToMoveTo(to)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(to);
    Arrays.stream(listeners)
      .forEach(t -> assertThat(t.states).endsWith(to));
    return lifecycle;
  }

  /**
   * Creates a Lifecycle object in the specified state emulating that the shortest path to this state has been followed
   * to reach it.
   */
  private static ManagedProcessLifecycle newLifeCycle(State state, TestLifeCycleListener... listeners) {
    switch (state) {
      case INIT:
        return new ManagedProcessLifecycle(ProcessId.ELASTICSEARCH, asList(listeners));
      case STARTING:
        return newLifeCycle(INIT, state, listeners);
      case STARTED:
        return newLifeCycle(STARTING, state, listeners);
      case STOPPING:
        return newLifeCycle(STARTED, state, listeners);
      case HARD_STOPPING:
        return newLifeCycle(STARTED, state, listeners);
      case FINALIZE_STOPPING:
        return newLifeCycle(STOPPING, state, listeners);
      case STOPPED:
        return newLifeCycle(FINALIZE_STOPPING, state, listeners);
      default:
        throw new IllegalStateException("new state is not supported:" + state);
    }
  }

  @DataProvider
  public static Object[][] allStates() {
    return Arrays.stream(State.values())
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private static final class TestLifeCycleListener implements ProcessLifecycleListener {
    private final List<State> states = new ArrayList<>();

    @Override
    public void onProcessState(ProcessId processId, State state) {
      this.states.add(state);
    }
  }
}

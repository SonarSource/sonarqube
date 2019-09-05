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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.application.NodeLifecycle.State.FINALIZE_STOPPING;
import static org.sonar.application.NodeLifecycle.State.HARD_STOPPING;
import static org.sonar.application.NodeLifecycle.State.INIT;
import static org.sonar.application.NodeLifecycle.State.OPERATIONAL;
import static org.sonar.application.NodeLifecycle.State.RESTARTING;
import static org.sonar.application.NodeLifecycle.State.STARTING;
import static org.sonar.application.NodeLifecycle.State.STOPPED;
import static org.sonar.application.NodeLifecycle.State.STOPPING;

@RunWith(DataProviderRunner.class)
public class NodeLifecycleTest {
  private NodeLifecycle underTest = new NodeLifecycle();

  @Test
  public void verify_regular_start_and_graceful_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  public void verify_regular_start_and_early_graceful_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  public void verify_start_and_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(HARD_STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  public void verify_start_graceful_stop_interrupted_by_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(STOPPING);
    verifyMoveTo(HARD_STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  public void verify_regular_start_restart_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(RESTARTING);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
  }

  @Test
  public void verify_failed_restart_resulting_in_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(RESTARTING);
    verifyMoveTo(HARD_STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  public void verify_failed_restart_even_if_FINALIZE_STOPPING_was_initiated_resulting_in_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    verifyMoveTo(STARTING);
    verifyMoveTo(OPERATIONAL);
    verifyMoveTo(RESTARTING);
    verifyMoveTo(HARD_STOPPING);
    verifyMoveTo(FINALIZE_STOPPING);
    verifyMoveTo(STOPPED);
  }

  @Test
  @UseDataProvider("allStates")
  public void RESTARTING_is_only_allowed_from_STARTING_and_OPERATIONAL(NodeLifecycle.State state) {
    if (state == STARTING || state == OPERATIONAL) {
      verifyMoveTo(newNodeLifecycle(state), RESTARTING);
    } else {
      assertThat(newNodeLifecycle(state).tryToMoveTo(RESTARTING)).isFalse();
    }
  }

  /**
   * Moving to stopped displays a log "SQ stopped" which must no appear when restarting
   */
  @Test
  public void STOPPED_is_not_allowed_from_RESTARTING() {
    assertThat(newNodeLifecycle(RESTARTING).tryToMoveTo(STOPPED)).isFalse();
  }

  /**
   * To go to STOPPED state, one must go through FINALIZE_STOPPING
   */
  @Test
  @UseDataProvider("allStates")
  public void STOPPED_is_allowed_only_from_FINALIZE_STOPPING(NodeLifecycle.State state) {
    if (state == FINALIZE_STOPPING) {
      verifyMoveTo(newNodeLifecycle(state), STOPPED);
    } else {
      assertThat(newNodeLifecycle(state).tryToMoveTo(STOPPED)).isFalse();
    }
  }

  @Test
  @UseDataProvider("allStates")
  public void FINALIZE_STOPPING_is_only_allowed_from_STOPPING_and_HARD_STOPPING(NodeLifecycle.State state) {
    if (state == STOPPING || state == HARD_STOPPING) {
      verifyMoveTo(newNodeLifecycle(state), FINALIZE_STOPPING);
    } else {
      assertThat(newNodeLifecycle(state).tryToMoveTo(FINALIZE_STOPPING)).isFalse();
    }
  }

  @Test
  @UseDataProvider("allStates")
  public void cannot_move_to_any_state_from_STOPPED(NodeLifecycle.State state) {
    assertThat(newNodeLifecycle(STOPPED).tryToMoveTo(state)).isFalse();
  }

  private void verifyMoveTo(NodeLifecycle.State newState) {
    verifyMoveTo(this.underTest, newState);
  }

  private void verifyMoveTo(NodeLifecycle lifecycle, NodeLifecycle.State newState) {
    assertThat(lifecycle.tryToMoveTo(newState)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(newState);
  }

  /**
   * Creates a Lifecycle object in the specified state emulating that the shortest path to this state has been followed
   * to reach it.
   */
  private static NodeLifecycle newNodeLifecycle(NodeLifecycle.State state) {
    switch (state) {
      case INIT:
        return new NodeLifecycle();
      case STARTING:
        return newNodeLifecycle(INIT, state);
      case OPERATIONAL:
        return newNodeLifecycle(STARTING, state);
      case RESTARTING:
        return newNodeLifecycle(OPERATIONAL, state);
      case STOPPING:
        return newNodeLifecycle(OPERATIONAL, state);
      case HARD_STOPPING:
        return newNodeLifecycle(OPERATIONAL, state);
      case FINALIZE_STOPPING:
        return newNodeLifecycle(STOPPING, state);
      case STOPPED:
        return newNodeLifecycle(FINALIZE_STOPPING, state);
      default:
        throw new IllegalStateException("Missing state! " + state);
    }
  }

  private static NodeLifecycle newNodeLifecycle(NodeLifecycle.State from, NodeLifecycle.State to) {
    NodeLifecycle lifecycle = newNodeLifecycle(from);
    assertThat(lifecycle.tryToMoveTo(to)).isTrue();
    return lifecycle;
  }

  @DataProvider
  public static Object[][] allStates() {
    return Arrays.stream(NodeLifecycle.State.values())
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }
}

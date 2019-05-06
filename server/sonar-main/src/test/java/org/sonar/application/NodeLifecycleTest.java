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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.application.NodeLifecycle.State.HARD_STOPPING;
import static org.sonar.application.NodeLifecycle.State.INIT;
import static org.sonar.application.NodeLifecycle.State.OPERATIONAL;
import static org.sonar.application.NodeLifecycle.State.RESTARTING;
import static org.sonar.application.NodeLifecycle.State.STARTING;
import static org.sonar.application.NodeLifecycle.State.STOPPED;
import static org.sonar.application.NodeLifecycle.State.STOPPING;

public class NodeLifecycleTest {
  private NodeLifecycle underTest = new NodeLifecycle();

  @Test
  public void verify_regular_start_and_graceful_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
    assertThat(underTest.tryToMoveTo(STOPPING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPING);
    assertThat(underTest.tryToMoveTo(STOPPED)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPED);
  }

  @Test
  public void verify_start_and_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
    assertThat(underTest.tryToMoveTo(HARD_STOPPING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(HARD_STOPPING);
    assertThat(underTest.tryToMoveTo(STOPPED)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPED);
  }

  @Test
  public void verify_start_graceful_stop_interrupted_by_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
    assertThat(underTest.tryToMoveTo(STOPPING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPING);
    assertThat(underTest.tryToMoveTo(HARD_STOPPING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(HARD_STOPPING);
    assertThat(underTest.tryToMoveTo(STOPPED)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPED);
  }

  @Test
  public void verify_regular_start_restart_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
    assertThat(underTest.tryToMoveTo(RESTARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(RESTARTING);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
  }

  @Test
  public void verify_failed_restart_resulting_in_hard_stop_cycle() {
    assertThat(underTest.getState()).isEqualTo(INIT);
    assertThat(underTest.tryToMoveTo(STARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STARTING);
    assertThat(underTest.tryToMoveTo(OPERATIONAL)).isTrue();
    assertThat(underTest.getState()).isEqualTo(OPERATIONAL);
    assertThat(underTest.tryToMoveTo(RESTARTING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(RESTARTING);
    assertThat(underTest.tryToMoveTo(HARD_STOPPING)).isTrue();
    assertThat(underTest.getState()).isEqualTo(HARD_STOPPING);
    assertThat(underTest.tryToMoveTo(STOPPED)).isTrue();
    assertThat(underTest.getState()).isEqualTo(STOPPED);
  }
}

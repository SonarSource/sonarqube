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
package org.sonar.server.issue.workflow;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StateMachineTest {
  @Test
  public void keep_order_of_state_keys() {
    StateMachine machine = StateMachine.builder().states("OPEN", "RESOLVED", "CLOSED").build();

    assertThat(machine.stateKeys()).containsSubsequence("OPEN", "RESOLVED", "CLOSED");
  }

  @Test
  public void stateKey() {
    StateMachine machine = StateMachine.builder()
      .states("OPEN", "RESOLVED", "CLOSED")
      .transition(Transition.builder("resolve").from("OPEN").to("RESOLVED").build())
      .build();

    assertThat(machine.state("OPEN")).isNotNull();
    assertThat(machine.state("OPEN").transition("resolve")).isNotNull();
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StateTest {


  private Transition t1 = Transition.builder("close").from("OPEN").to("CLOSED").build();

  @Test
  public void key_should_be_set() {
    assertThatThrownBy(() -> new State("", new Transition[0]))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("State key must be set");
  }

  @Test
  public void no_duplicated_out_transitions() {
    assertThatThrownBy(() -> new State("CLOSE", new Transition[] {t1, t1}))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition 'close' is declared several times from the originating state 'CLOSE'");
  }

  @Test
  public void fail_when_transition_is_unknown() {
    State state = new State("VALIDATED", new Transition[0]);

    assertThatThrownBy(() -> state.transition("Unknown Transition"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition from state VALIDATED does not exist: Unknown Transition");
  }
}

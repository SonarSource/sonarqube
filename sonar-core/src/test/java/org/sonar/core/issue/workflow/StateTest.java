/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue.workflow;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class StateTest {

  Transition t1 = Transition.builder("close").from("OPEN").to("CLOSED").build();

  @Test
  public void key_should_be_set() throws Exception {
    try {
      new State("", new Transition[0]);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("State key must be set");
    }
  }

  @Test
  public void key_should_be_upper_case() throws Exception {
    try {
      new State("close", new Transition[0]);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("State key must be upper-case");
    }
  }

  @Test
  public void no_duplicated_out_transitions() throws Exception {
    try {
      new State("CLOSE", new Transition[]{t1, t1});
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Transition 'close' is declared several times from the originating state 'CLOSE'");
    }
  }
}

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
package org.sonar.process;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.Lifecycle.State;

public class LifecycleTest {

  @Test
  public void equals_and_hashcode() throws Exception {
    Lifecycle init = new Lifecycle();
    assertThat(init.equals(init)).isTrue();
    assertThat(init.equals(new Lifecycle())).isTrue();
    assertThat(init.equals("INIT")).isFalse();
    assertThat(init.equals(null)).isFalse();
    assertThat(init.hashCode()).isEqualTo(new Lifecycle().hashCode());

    // different state
    Lifecycle stopping = new Lifecycle();
    stopping.tryToMoveTo(State.STOPPING);
    assertThat(stopping).isNotEqualTo(init);
  }

  @Test
  public void try_to_move() throws Exception {
    Lifecycle lifecycle = new Lifecycle();
    assertThat(lifecycle.getState()).isEqualTo(State.INIT);

    assertThat(lifecycle.tryToMoveTo(State.STARTED)).isTrue();
    assertThat(lifecycle.getState()).isEqualTo(State.STARTED);

    assertThat(lifecycle.tryToMoveTo(State.STARTING)).isFalse();
    assertThat(lifecycle.getState()).isEqualTo(State.STARTED);
  }
}

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
package org.sonar.core.graph;

import com.tinkerpop.blueprints.Direction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class EdgePathTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_valid_edge_path() {
    EdgePath path = EdgePath.create(Direction.OUT, "knows", Direction.OUT, "has");

    assertThat(path).isNotNull();
    assertThat(path.getElements()).hasSize(4);
    assertThat(path.getElements().get(0)).isEqualTo(Direction.OUT);
    assertThat(path.getElements().get(1)).isEqualTo("knows");
    assertThat(path.getElements().get(2)).isEqualTo(Direction.OUT);
    assertThat(path.getElements().get(3)).isEqualTo("has");
  }

  @Test
  public void should_have_even_number_of_elements() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Odd number of elements (3)");
    EdgePath.create(Direction.OUT, "knows", Direction.OUT);
  }

  @Test
  public void should_have_sequence_of_directions_and_strings_1() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Element 0 must be a com.tinkerpop.blueprints.Direction (got java.lang.String)");

    EdgePath.create("knows", Direction.OUT);
  }

  @Test
  public void should_have_sequence_of_directions_and_strings_2() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Element 1 must be a String (got com.tinkerpop.blueprints.Direction)");

    EdgePath.create(Direction.OUT, Direction.OUT);
  }
}

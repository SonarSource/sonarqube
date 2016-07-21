/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.filemove;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceSimilarityImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SourceSimilarityImpl underTest = new SourceSimilarityImpl();

  @Test
  public void zero_if_fully_different() {
    List<String> left = asList("a", "b", "c");
    List<String> right = asList("d", "e");
    assertThat(underTest.score(left, right)).isEqualTo(0);
  }

  @Test
  public void one_hundred_if_same() {
    assertThat(underTest.score(asList("a", "b", "c"), asList("a", "b", "c"))).isEqualTo(100);
    assertThat(underTest.score(asList(""), asList(""))).isEqualTo(100);
  }

  @Test
  public void partially_same() {
    assertThat(underTest.score(asList("a", "b", "c", "d"), asList("a", "b", "e", "f"))).isEqualTo(50);
    assertThat(underTest.score(asList("a"), asList("a", "b", "c"))).isEqualTo(33);
    assertThat(underTest.score(asList("a", "b", "c"), asList("a"))).isEqualTo(33);
  }
}

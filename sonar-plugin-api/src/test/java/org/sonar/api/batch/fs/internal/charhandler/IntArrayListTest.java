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
package org.sonar.api.batch.fs.internal.charhandler;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.charhandler.IntArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class IntArrayListTest {

  @Test
  public void addElements() {
    IntArrayList list = new IntArrayList();
    assertThat(list.trimAndGet()).isEmpty();
    list.add(1);
    list.add(2);
    assertThat(list.trimAndGet()).containsExactly(1, 2);
  }

  @Test
  public void trimIfNeeded() {
    IntArrayList list = new IntArrayList();
    list.add(1);
    list.add(2);
    assertThat(list.trimAndGet()).isSameAs(list.trimAndGet());
  }

  @Test
  public void grow() {
    // Default capacity is 10
    IntArrayList list = new IntArrayList();
    for (int i = 1; i <= 11; i++) {
      list.add(i);
    }
    assertThat(list.trimAndGet()).hasSize(11);
  }

}

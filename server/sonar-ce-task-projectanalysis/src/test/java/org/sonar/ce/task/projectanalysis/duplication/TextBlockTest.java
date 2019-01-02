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
package org.sonar.ce.task.projectanalysis.duplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TextBlockTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_IAE_if_start_is_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("First line index must be >= 1");

    new TextBlock(0, 2);
  }

  @Test
  public void constructor_throws_IAE_if_end_is_less_than_start() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Last line index must be >= first line index");

    new TextBlock(1, 0);
  }

  @Test
  public void getStart_returns_constructor_argument() {
    TextBlock textBlock = new TextBlock(15, 300);

    assertThat(textBlock.getStart()).isEqualTo(15);
  }

  @Test
  public void getEnd_returns_constructor_argument() {
    TextBlock textBlock = new TextBlock(15, 300);

    assertThat(textBlock.getEnd()).isEqualTo(300);
  }

  @Test
  public void equals_compares_on_start_and_end() {
    assertThat(new TextBlock(15, 15)).isEqualTo(new TextBlock(15, 15));
    assertThat(new TextBlock(15, 300)).isEqualTo(new TextBlock(15, 300));
    assertThat(new TextBlock(15, 300)).isNotEqualTo(new TextBlock(15, 15));
  }

  @Test
  public void hashcode_is_based__on_start_and_end() {
    assertThat(new TextBlock(15, 15).hashCode()).isEqualTo(new TextBlock(15, 15).hashCode());
    assertThat(new TextBlock(15, 300).hashCode()).isEqualTo(new TextBlock(15, 300).hashCode());
    assertThat(new TextBlock(15, 300).hashCode()).isNotEqualTo(new TextBlock(15, 15).hashCode());
  }

  @Test
  public void TextBlock_defines_natural_order_by_start_then_end() {
    TextBlock textBlock1 = new TextBlock(1, 1);
    TextBlock textBlock2 = new TextBlock(1, 2);
    TextBlock textBlock3 = new TextBlock(2, 3);
    TextBlock textBlock4 = new TextBlock(2, 4);
    TextBlock textBlock5 = new TextBlock(5, 5);

    List<TextBlock> shuffledList = new ArrayList<>(Arrays.asList(textBlock1, textBlock2, textBlock3, textBlock4, textBlock5));
    Collections.shuffle(shuffledList, new Random());

    Collections.sort(shuffledList);
    assertThat(shuffledList).containsExactly(textBlock1, textBlock2, textBlock3, textBlock4, textBlock5);
  }

  @Test
  public void verify_toString() {
    assertThat(new TextBlock(13, 400).toString()).isEqualTo("TextBlock{start=13, end=400}");

  }
}

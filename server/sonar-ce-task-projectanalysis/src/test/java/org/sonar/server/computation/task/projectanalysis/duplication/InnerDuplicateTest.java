/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.duplication;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerDuplicateTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructors_throws_NPE_if_textBlock_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("textBlock of duplicate can not be null");

    new InnerDuplicate(null);
  }

  @Test
  public void getTextBlock_returns_TextBlock_constructor_argument() {
    TextBlock textBlock = new TextBlock(2, 3);
    assertThat(new InnerDuplicate(textBlock).getTextBlock()).isSameAs(textBlock);
  }

  @Test
  public void equals_compares_on_TextBlock() {
    assertThat(new InnerDuplicate(new TextBlock(1, 2))).isEqualTo(new InnerDuplicate(new TextBlock(1, 2)));
    assertThat(new InnerDuplicate(new TextBlock(1, 2))).isNotEqualTo(new InnerDuplicate(new TextBlock(1, 1)));
  }

  @Test
  public void hashcode_is_TextBlock_hashcode() {
    TextBlock textBlock = new TextBlock(1, 2);
    assertThat(new InnerDuplicate(textBlock).hashCode()).isEqualTo(textBlock.hashCode());
  }

  @Test
  public void verify_toString() {
    assertThat(new InnerDuplicate(new TextBlock(1, 2)).toString()).isEqualTo("InnerDuplicate{textBlock=TextBlock{start=1, end=2}}");
  }
}

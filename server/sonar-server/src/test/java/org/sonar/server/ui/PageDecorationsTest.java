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
package org.sonar.server.ui;

import org.junit.Test;
import org.sonar.api.web.PageDecoration;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PageDecorationsTest {

  @Test
  public void should_not_fail_if_no_decorations() {
    assertThat(new PageDecorations().get()).isEmpty();
  }

  @Test
  public void should_register_decorations() {
    PageDecoration deco1 = mock(PageDecoration.class);
    PageDecoration deco2 = mock(PageDecoration.class);

    PageDecorations decorations = new PageDecorations(Arrays.asList(deco1, deco2));

    assertThat(decorations.get()).hasSize(2);
  }
}

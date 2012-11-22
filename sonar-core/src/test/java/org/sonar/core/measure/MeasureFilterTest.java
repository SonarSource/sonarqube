/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureFilterTest {
  @Test
  public void should_sanitize_list() {
    assertThat(MeasureFilter.sanitize(null)).isEmpty();
    assertThat(MeasureFilter.sanitize(Lists.<String>newArrayList())).isEmpty();
    assertThat(MeasureFilter.sanitize(Arrays.asList(""))).isEmpty();
    assertThat(MeasureFilter.sanitize(Lists.newArrayList("TRK"))).containsExactly("TRK");
    assertThat(MeasureFilter.sanitize(Lists.newArrayList("TRK", "BRC"))).containsExactly("TRK", "BRC");
  }
}

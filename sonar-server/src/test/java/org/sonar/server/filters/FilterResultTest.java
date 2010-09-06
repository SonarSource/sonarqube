/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.filters;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class FilterResultTest {

  @Test
  public void sortWithNullElements() {
    List<String[]> list = Arrays.asList(new String[]{"foo"}, new String[]{null}, new String[]{"bar"}, new String[]{null}, new String[]{null}, new String[]{"toto"});
    Collections.sort(list, new FilterResult.RowComparator(0));

    assertThat(list.get(0)[0], nullValue());
    assertThat(list.get(1)[0], nullValue());
    assertThat(list.get(2)[0], nullValue());
    assertThat(list.get(3)[0], is("bar"));
    assertThat(list.get(4)[0], is("foo"));
    assertThat(list.get(5)[0], is("toto"));
  }
}

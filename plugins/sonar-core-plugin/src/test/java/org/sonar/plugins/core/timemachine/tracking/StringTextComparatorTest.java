/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.timemachine.tracking;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StringTextComparatorTest {

  @Test
  public void testEquals() {
    StringTextComparator cmp = StringTextComparator.IGNORE_WHITESPACE;

    StringText a = new StringText("abc\nabc\na bc");
    StringText b = new StringText("abc\nabc d\nab c");

    assertThat("abc == abc", cmp.equals(a, 0, b, 0), is(true));
    assertThat("abc != abc d", cmp.equals(a, 1, b, 1), is(false));
    assertThat("a bc == ab c", cmp.equals(a, 2, b, 2), is(true));
    assertThat(cmp.hash(a, 0), equalTo(cmp.hash(b, 0)));
    assertThat(cmp.hash(a, 2), equalTo(cmp.hash(b, 2)));
  }

}

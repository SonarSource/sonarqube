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
package org.sonar.colorizer;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlCodeBuilderTest {

  private HtmlCodeBuilder builder;

  @Before
  public void init() {
    builder = new HtmlCodeBuilder();
  }

  @Test
  public void testAppendCharSequence() {
    builder.append("freddy < olivier");
    assertThat("freddy &lt; olivier").isEqualTo(builder.toString());
  }

  @Test
  public void testAppendChar() {
    builder.append('p');
    builder.append('a');
    builder.append('>');
    assertThat("pa&gt;").isEqualTo(builder.toString());
  }

  @Test
  public void testAppendCharSequenceIntInt() {
    builder.append("freddy < olivier", 0, 2);
    assertThat("fr").isEqualTo(builder.toString());
  }

  @Test
  public void testAppendWithoutTransforming() {
    builder.appendWithoutTransforming("<inside>outside");
    assertThat("<inside>outside").isEqualTo(builder.toString());
  }

  @Test
  public void testStatefulVariables() {
    assertThat(builder.getVariable("foo")).isNull();

    builder.setVariable("foo", "xxx");
    assertThat((String) builder.getVariable("foo")).isEqualTo(("xxx"));

    builder.setVariable("foo", "yyy");
    assertThat((String) builder.getVariable("foo")).isEqualTo(("yyy"));

    builder.setVariable("foo", null);
    assertThat(builder.getVariable("foo")).isNull();

    assertThat((String) builder.getVariable("foo", "default")).isEqualTo(("default"));
  }

}

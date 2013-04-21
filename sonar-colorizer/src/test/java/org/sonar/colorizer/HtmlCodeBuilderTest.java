/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.colorizer;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HtmlCodeBuilderTest {

  private HtmlCodeBuilder builder;

  @Before
  public void init() {
    builder = new HtmlCodeBuilder();
  }

  @Test
  public void testAppendCharSequence() {
    builder.append("freddy < olivier");
    assertEquals("freddy &lt; olivier", builder.toString());
  }

  @Test
  public void testAppendChar() {
    builder.append('p');
    builder.append('a');
    builder.append('>');
    assertEquals("pa&gt;", builder.toString());
  }

  @Test
  public void testAppendCharSequenceIntInt() {
    builder.append("freddy < olivier", 0, 2);
    assertEquals("fr", builder.toString());
  }

  @Test
  public void testAppendWithoutTransforming() {
    builder.appendWithoutTransforming("<inside>outside");
    assertEquals("<inside>outside", builder.toString());
  }

  @Test
  public void testStatefulVariables() {
    assertThat(builder.getVariable("foo"), nullValue());

    builder.setVariable("foo", "xxx");
    assertThat((String) builder.getVariable("foo"), is("xxx"));

    builder.setVariable("foo", "yyy");
    assertThat((String) builder.getVariable("foo"), is("yyy"));

    builder.setVariable("foo", null);
    assertThat(builder.getVariable("foo"), nullValue());

    assertThat((String) builder.getVariable("foo", "default"), is("default"));
  }

}

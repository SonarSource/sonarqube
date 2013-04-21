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

package org.sonar.squid.text;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiteralValueHandlerTest {

  @Test
  public void matchToBegin() {
    LiteralValueHandler handler = new LiteralValueHandler('"');
    assertTrue(handler.matchToBegin(new Line(), new StringBuilder("toto = \"")));
    assertFalse(handler.matchToBegin(new Line(), new StringBuilder("toto = \'")));
  }

  @Test
  public void matchToEnd() {
    LiteralValueHandler handler = new LiteralValueHandler('"');
    assertTrue(handler.matchToEnd(new Line(), new StringBuilder("toto = \"lklj\"")));
    assertFalse(handler.matchToEnd(new Line(), new StringBuilder("\\\"")));
    assertTrue(handler.matchToEnd(new Line(), new StringBuilder("\\\\\"")));
    assertFalse(handler.matchToEnd(new Line(), new StringBuilder("\\\\\\\"")));
    assertTrue(handler.matchToEnd(new Line(), new StringBuilder("\\\\\\\\\"")));
    assertFalse(handler.matchToEnd(new Line(), new StringBuilder("toto = \'")));
  }

  @Test
  public void matchToEndOfLine() {
    LiteralValueHandler handler = new LiteralValueHandler('"');
    assertTrue(handler.matchWithEndOfLine(new Line(), new StringBuilder()));
  }

}

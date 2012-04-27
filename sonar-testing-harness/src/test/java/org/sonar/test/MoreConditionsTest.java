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
package org.sonar.test;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.test.MoreConditions.equalsIgnoreEOL;

public class MoreConditionsTest {
  @Test
  public void should_compare_equals_texts() {
    assertThat("TEXT").satisfies(equalsIgnoreEOL("TEXT"));
  }

  @Test
  public void should_ignore_line_feeds_and_carriage_returns() {
    assertThat("BEFORE\nAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFORE\rAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFORE\n\rAFTER").satisfies(equalsIgnoreEOL("BEFOREAFTER"));
    assertThat("BEFOREAFTER").satisfies(equalsIgnoreEOL("BEFORE\n\rAFTER"));
  }

  @Test
  public void should_refuse_different_values() {
    assertThat("TEXT").doesNotSatisfy(equalsIgnoreEOL("DIFFERENT"));
  }

  @Test
  public void should_accept_empty_values() {
    assertThat("").satisfies(equalsIgnoreEOL(""));
    assertThat("").satisfies(equalsIgnoreEOL("\n\r"));
    assertThat("\n\r").satisfies(equalsIgnoreEOL(""));
  }
}

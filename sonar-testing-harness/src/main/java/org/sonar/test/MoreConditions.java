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

import com.google.common.base.CharMatcher;
import org.fest.assertions.Condition;

/**
 * Conditions for use with FestAssert.
 */
public final class MoreConditions {
  private static CharMatcher EOLS = CharMatcher.anyOf("\n\r");

  private MoreConditions() {
    // static utility class
  }

  public static Condition<String> equalsIgnoreEOL(String text) {
    final String strippedText = EOLS.removeFrom(text);

    return new Condition<String>() {
      @Override
      public boolean matches(String value) {
        return EOLS.removeFrom(value).equals(strippedText);
      }
    }.as("equal to " + strippedText);
  }
}

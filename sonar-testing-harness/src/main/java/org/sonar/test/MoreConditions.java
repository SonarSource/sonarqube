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
package org.sonar.test;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.fest.assertions.Condition;

import java.util.Collection;

/**
 * Conditions for use with FestAssert.
 */
public final class MoreConditions {
  private static final CharMatcher EOLS = CharMatcher.anyOf("\n\r");

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

  public static Condition<Collection<?>> contains(final Object expected) {
    return new Condition<Collection<?>>() {
      @Override
      public boolean matches(Collection<?> collection) {
        for (Object actual : collection) {
          if (EqualsBuilder.reflectionEquals(expected, actual)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static Condition<Object> reflectionEqualTo(final Object expected) {
    return new Condition<Object>() {
      @Override
      public boolean matches(Object actual) {
        return EqualsBuilder.reflectionEquals(expected, actual);
      }
    };
  }
}

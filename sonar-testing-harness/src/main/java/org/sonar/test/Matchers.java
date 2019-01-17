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
package org.sonar.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Utility class to provide various Matchers to use in ExpectedException.expectMessage.
 */
public class Matchers {

  private Matchers() {
    // utility class, forbidden constructor
  }

  public static Matcher<String> regexMatcher(String regex) {
    return new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String item) {
        return item.matches(regex);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("matching regex ").appendValue(regex);
      }
    };
  }
}

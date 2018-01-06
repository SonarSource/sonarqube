/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.duplications.detector;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import static org.hamcrest.CoreMatchers.hasItem;

public class CloneGroupMatcher extends TypeSafeMatcher<CloneGroup> {

  public static Matcher<Iterable<? super CloneGroup>> hasCloneGroup(int expectedLen, ClonePart... expectedParts) {
    return hasItem(new CloneGroupMatcher(expectedLen, expectedParts));
  }

  private final int expectedLen;
  private final ClonePart[] expectedParts;

  private CloneGroupMatcher(int expectedLen, ClonePart... expectedParts) {
    this.expectedLen = expectedLen;
    this.expectedParts = expectedParts;
  }

  @Override
  public boolean matchesSafely(CloneGroup cloneGroup) {
    // Check length
    if (expectedLen != cloneGroup.getCloneUnitLength()) {
      return false;
    }
    // Check number of parts
    if (expectedParts.length != cloneGroup.getCloneParts().size()) {
      return false;
    }
    // Check origin
    if (!expectedParts[0].equals(cloneGroup.getOriginPart())) {
      return false;
    }
    // Check parts
    for (ClonePart expectedPart : expectedParts) {
      boolean matched = false;
      for (ClonePart part : cloneGroup.getCloneParts()) {
        if (part.equals(expectedPart)) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }
    return true;
  }

  public void describeTo(Description description) {
    StringBuilder builder = new StringBuilder();
    for (ClonePart part : expectedParts) {
      builder.append(part).append(" - ");
    }
    builder.append(expectedLen);
    description.appendText(builder.toString());
  }

}

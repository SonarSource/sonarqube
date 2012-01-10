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
package org.sonar.duplications.utils;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sonar.duplications.utils.FastStringComparator;

public class FastStringComparatorTest {

  @Test
  public void sameHashCode() {
    // Next two Strings have same hash code in Java - see http://www.drmaciver.com/2008/07/javalangstringhashcode/
    String s1 = "Od";
    String s2 = "PE";
    assertTrue("same hash code", s1.hashCode() == s2.hashCode());
    assertThat("s1 < s2", FastStringComparator.INSTANCE.compare(s1, s2), lessThan(0));
    assertThat("s2 > s1", FastStringComparator.INSTANCE.compare(s2, s1), greaterThan(0));
  }

  @Test
  public void differentHashCode() {
    String s1 = "a";
    String s2 = "c";
    assertTrue("different hash code", s1.hashCode() != s2.hashCode());
    assertThat("s1 < s2", FastStringComparator.INSTANCE.compare(s1, s2), is(-1));
    assertThat("s2 > s1", FastStringComparator.INSTANCE.compare(s2, s1), is(1));
  }

  @Test
  public void sameObject() {
    String s1 = "a";
    String s2 = s1;
    assertTrue("same objects", s1 == s2);
    assertThat("s1 = s2", FastStringComparator.INSTANCE.compare(s1, s2), is(0));
    assertThat("s2 = s1", FastStringComparator.INSTANCE.compare(s2, s1), is(0));
  }

  @Test
  public void sameString() {
    String s1 = new String("a");
    String s2 = new String("a");
    assertTrue("different objects", s1 != s2);
    assertThat("s1 = s2", FastStringComparator.INSTANCE.compare(s1, s2), is(0));
    assertThat("s2 = s1", FastStringComparator.INSTANCE.compare(s2, s1), is(0));
  }

}

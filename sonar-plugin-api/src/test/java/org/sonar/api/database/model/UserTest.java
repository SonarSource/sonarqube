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
package org.sonar.api.database.model;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class UserTest {

  @Test
  public void testToString() {
    User user = new User()
        .setEmail("super@m.an")
        .setLogin("superman")
        .setName("Superman");
    assertThat(user.toString(), containsString("super@m.an"));
    assertThat(user.toString(), containsString("superman"));
    assertThat(user.toString(), containsString("Superman"));
  }

  @Test
  public void testEquals() {
    User one = new User()
        .setLogin("one")
        .setName("One");

    User two = new User()
        .setLogin("two")
        .setName("Two");

    assertThat(one.equals(one), Is.is(true));
    assertThat(one.equals(new User().setLogin("one")), Is.is(true));
    assertThat(one.equals(two), Is.is(false));

    assertEquals(one.hashCode(), new User().setLogin("one").hashCode());
  }
}

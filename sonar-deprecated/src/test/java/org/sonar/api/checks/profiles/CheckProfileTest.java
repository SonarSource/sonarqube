/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.checks.profiles;

import org.junit.Test;

import static junit.framework.Assert.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CheckProfileTest {

  @Test
  public void testCheck() {
    CheckProfile profile = new CheckProfile("fake", "java");
    assertThat(profile.getName(), is("fake"));
    assertThat(profile.getLanguage(), is("java"));
    assertThat(profile.getChecks().size(), is(0));
  }

  @Test
  public void equalsByNameAndLanguage() {
    CheckProfile profile1 = new CheckProfile("fake1", "java");
    CheckProfile profile1Clone = new CheckProfile("fake1", "java");
    CheckProfile profile2 = new CheckProfile("fake1", "php");

    assertTrue(profile1.equals(profile1));
    assertTrue(profile1.equals(profile1Clone));
    assertFalse(profile1.equals(profile2));

    assertEquals(profile1.hashCode(), profile1Clone.hashCode());
  }

  @Test
  public void addChecks() {
    CheckProfile profile = new CheckProfile("fake", "java");
    profile.addCheck(new Check("repo", "template"));

    assertThat(profile.getChecks().size(), is(1));
    assertThat(profile.getChecks("repo").size(), is(1));
    assertThat(profile.getChecks("other repo").size(), is(0));
  }
}

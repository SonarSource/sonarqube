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
package org.sonar.api.resources;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class LibraryTest {

  @Test
  public void equalsOnKeyAndVersion() {
    assertTrue(new Library("commons-lang", "1.1").equals(new Library("commons-lang", "1.1")));
    assertFalse(new Library("commons-lang", "1.1").equals(new Library("commons-lang", "1.0")));
  }

  @Test
  public void testHashCode() {
    assertThat(new Library("commons-lang", "1.1").hashCode(), is(new Library("commons-lang", "1.1").hashCode()));
    assertThat(new Library("commons-lang", "1.1").hashCode(), not(is(new Library("commons-lang", "1.0").hashCode())));
  }
}

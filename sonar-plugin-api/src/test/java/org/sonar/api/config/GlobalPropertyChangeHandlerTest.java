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
package org.sonar.api.config;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class GlobalPropertyChangeHandlerTest {
  @Test
  public void propertyChangeToString() {
    GlobalPropertyChangeHandler.PropertyChange change = GlobalPropertyChangeHandler.PropertyChange.create("favourite.java.version", "1.5");
    assertThat(change.toString(), is("[key=favourite.java.version, newValue=1.5]"));
  }

  @Test
  public void propertyChangeGetters() {
    GlobalPropertyChangeHandler.PropertyChange change = GlobalPropertyChangeHandler.PropertyChange.create("favourite.java.version", "1.5");
    assertThat(change.getKey(), is("favourite.java.version"));
    assertThat(change.getNewValue(), is("1.5"));
  }

  @Test
  public void nullNewValue() {
    GlobalPropertyChangeHandler.PropertyChange change = GlobalPropertyChangeHandler.PropertyChange.create("favourite.java.version", null);
    assertThat(change.getNewValue(), nullValue());
    assertThat(change.toString(), is("[key=favourite.java.version, newValue=null]"));
  }
}

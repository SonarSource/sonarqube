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

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;

public class CheckTest {

  @Test
  public void testParameters() {
    Check check = new Check("fake_plugin", "fake_key");
    assertThat(check.getProperties().size(), is(0));

    check.addProperty("foo", "bar");
    assertThat(check.getProperties().size(), is(1));
    assertThat(check.getProperties().get("foo"), is("bar"));

    Map<String, String> newParams = new HashMap<String, String>();
    newParams.put("foo", "new foo");
    newParams.put("hello", "world");

    check.setProperties(newParams);
    assertThat(check.getProperties().size(), is(2));
    assertThat(check.getProperties().get("foo"), is("new foo"));
    assertThat(check.getProperties().get("hello"), is("world"));
  }

  @Test
  public void equalsByReference() {
    Check check1 = new Check("fake_plugin", "fake_key");
    Check check2 = new Check("fake_plugin", "fake_key");

    assertTrue(check1.equals(check1));
    assertFalse(check1.equals(check2));
  }
}

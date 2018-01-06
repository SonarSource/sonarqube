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
package org.sonar.server.webhook;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Project underTest = new Project("a", "b", "c");

  @Test
  public void constructor_throws_NPE_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    new Project(null, "b", "c");
  }

  @Test
  public void constructor_throws_NPE_if_key_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    new Project("a", null, "c");
  }

  @Test
  public void constructor_throws_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    new Project("a", "b", null);
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.getUuid()).isEqualTo("a");
    assertThat(underTest.getKey()).isEqualTo("b");
    assertThat(underTest.getName()).isEqualTo("c");
  }
}

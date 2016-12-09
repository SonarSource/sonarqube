/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.projectanalysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateEventRequestTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdateEventRequest underTest;

  @Test
  public void request_with_name_only() {
    underTest = new UpdateEventRequest("E1", "name", null);

    assertThat(underTest.getEvent()).isEqualTo("E1");
    assertThat(underTest.getName()).isEqualTo("name");
    assertThat(underTest.getDescription()).isNull();
  }

  @Test
  public void request_with_description_only() {
    underTest = new UpdateEventRequest("E1", null , "description");

    assertThat(underTest.getEvent()).isEqualTo("E1");
    assertThat(underTest.getName()).isNull();
    assertThat(underTest.getDescription()).isEqualTo("description");
  }

  @Test
  public void request_with_all_parameters() {
    underTest = new UpdateEventRequest("E1", "name", "description");

    assertThat(underTest.getEvent()).isEqualTo("E1");
    assertThat(underTest.getName()).isEqualTo("name");
    assertThat(underTest.getDescription()).isEqualTo("description");
  }

  @Test
  public void fail_if_null_event() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Event key is required");

    new UpdateEventRequest(null, "name", "description");
  }

  @Test
  public void fail_if_name_and_description_not_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name or description is required");

    new UpdateEventRequest("E1", null, null);
  }
}

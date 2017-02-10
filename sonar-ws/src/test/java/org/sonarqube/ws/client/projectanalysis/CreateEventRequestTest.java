/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.projectanalysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.OTHER;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.VERSION;

public class CreateEventRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateEventRequest.Builder underTest = CreateEventRequest.builder();

  @Test
  public void build_request() {
    CreateEventRequest result = underTest.setAnalysis("P1").setCategory(OTHER).setName("name").build();

    assertThat(result.getAnalysis()).isEqualTo("P1");
    assertThat(result.getCategory()).isEqualTo(OTHER);
    assertThat(result.getName()).isEqualTo("name");
  }

  @Test
  public void other_category_by_default() {
    CreateEventRequest result = underTest.setAnalysis("P1").setName("name").build();

    assertThat(OTHER).isEqualTo(result.getCategory());
  }

  @Test
  public void fail_when_no_category() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Category is required");

    underTest.setAnalysis("P1").setName("name").setCategory(null).build();
  }

  @Test
  public void fail_when_no_analysis() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Analysis key is required");

    underTest.setCategory(VERSION).setName("name").build();
  }

  @Test
  public void fail_when_no_name() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name is required");

    underTest.setAnalysis("P1").setCategory(VERSION).build();
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.test.EventAssert.assertThatEvent;

public class EventAssertTest {
  @Test
  public void isValid_no_field() {
    assertThatThrownBy(() -> assertThatEvent("line without field").isValid())
      .isInstanceOf(AssertionError.class)
      .hasMessage("Invalid line in event: 'line without field'");
  }
  @Test
  public void isValid_unknown_field() {
    assertThatThrownBy(() -> assertThatEvent("field: value").isValid())
      .isInstanceOf(AssertionError.class)
      .hasMessage("Unknown field in event: 'field'");
  }

  @Test
  public void isValid_correct() {
    assertThatEvent("").isValid();
    assertThatEvent("\n\n").isValid();
    assertThatEvent("data: D").isValid();
    assertThatEvent("event: E\ndata: D").isValid();
  }

  @Test
  public void hasField_invalid() {
    assertThatThrownBy(() -> assertThatEvent("line without field").hasField("event"))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Invalid line in event: 'line without field'");
  }

  @Test
  public void hasField_no_field() {
    assertThatThrownBy(() -> assertThatEvent("event: E").hasField("data"))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Expected event to contain field 'data'. Actual event was: 'event: E'");
  }

  @Test
  public void hasField_correct() {
    assertThatEvent("event: E\ndata: D").hasField("data");
    assertThatEvent("event: E\ndata: D").hasField("event");
  }

  @Test
  public void hasType_invalid() {
    assertThatThrownBy(() -> assertThatEvent("line without field").hasType("E"))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Invalid line in event: 'line without field'");
  }

  @Test
  public void hasType_without_type() {
    assertThatThrownBy(() -> assertThatEvent("data: D").hasType("E"))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Expected event to contain field 'event'. Actual event was: 'data: D'");
  }

  @Test
  public void hasType_correct() {
    assertThatEvent("event: E\ndata: D").hasType("E");
  }

  @Test
  public void hasData_invalid() {
    assertThatThrownBy(() -> assertThatEvent("line without field").hasData("D"))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Invalid line in event: 'line without field'");
  }

  @Test
  public void hasData_correct() {
    assertThatEvent("data:D").hasData("D");
  }

  @Test
  public void hasJsonData_invalid() {
    assertThatThrownBy(() -> assertThatEvent("line without field").hasJsonData(getClass().getResource("EventAssertTest/sample.json")))
      .isInstanceOf(AssertionError.class)
      .hasMessage("Invalid line in event: 'line without field'");
  }

  @Test
  public void hasJsonData_correct() {
    assertThatEvent("data: {}").hasJsonData(getClass().getResource("EventAssertTest/sample.json"));
  }
}

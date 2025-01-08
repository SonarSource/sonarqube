/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.audit.model;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentNewValueTest {

  @Test
  void toString_generatesValidJson() throws ParseException {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "qualifier");

    JSONObject jsonObject = (JSONObject) new JSONParser().parse(newValue.toString());

    assertThat(jsonObject).hasSize(5);
  }

  @Test
  void toString_addsPortfolioQualifier() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "VW");

    assertThat(newValue.toString())
      .contains("componentUuid")
      .contains("\"qualifier\": \"portfolio\"");
  }

  @Test
  void toString_project_uuid_and_name_and_isPrivate_withEscapedQuotes() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "the \"best\" name", "key", true, "TRK");

    assertThat(newValue.toString())
      .contains("\"componentUuid\": \"uuid\"")
      .contains("\"componentKey\": \"key\"")
      .contains("\"componentName\": \"the \\\"best\\\" name\"")
      .contains("\"qualifier\": \"project\"")
      .contains("\"isPrivate\": true");
  }

  @Test
  void toString_project_uuid_and_name_and_key() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", "TRK");

    assertThat(newValue.toString())
      .contains("\"componentUuid\": \"uuid\"")
      .contains("\"componentName\": \"name\"")
      .contains("\"qualifier\": \"project\"")
      .contains("\"componentKey\": \"key\"");
  }

  @Test
  void toString_project_uuid_and_name_and_key_and_isPrivate_and_description() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", true, "name", "key", "description", "TRK");

    assertThat(newValue.toString())
      .contains("\"componentUuid\": \"uuid\"")
      .contains("\"componentName\": \"name\"")
      .contains("\"qualifier\": \"project\"")
      .contains("\"isPrivate\": true")
      .contains("\"componentKey\": \"key\"")
      .contains("\"description\": \"description\"");
  }

  @Test
  void toString_addsProjectQualifier() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "TRK");

    assertThat(newValue.toString())
      .contains("componentUuid")
      .contains("\"qualifier\": \"project\"");
  }

  @Test
  void toString_addsApplicationQualifier() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "APP");

    assertThat(newValue.toString())
      .contains("componentUuid")
      .contains("\"qualifier\": \"application\"");
  }

}

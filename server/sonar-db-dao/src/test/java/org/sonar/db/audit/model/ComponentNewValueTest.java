/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentNewValueTest {

  @Test
  public void toString_generatesValidJson() throws ParseException {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "qualifier");

    JSONObject jsonObject = (JSONObject) new JSONParser().parse(newValue.toString());

    assertThat(jsonObject.size()).isEqualTo(5);
  }

  @Test
  public void toString_addsPortfolioPrefix() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "VW");

    assertThat(newValue.toString()).contains("portfolioUuid");
  }

  @Test
  public void toString_project_uuid_and_name_and_isPrivate() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", true,"TRK");

    assertThat(newValue.toString()).contains("\"projectUuid\": \"uuid\"");
    assertThat(newValue.toString()).contains("\"projectName\": \"name\"");
    assertThat(newValue.toString()).contains("\"isPrivate\": true");
  }

  @Test
  public void toString_addsProjectPrefix() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "TRK");

    assertThat(newValue.toString()).contains("projectUuid");
  }

  @Test
  public void toString_addsApplicationPrefix() {
    ComponentNewValue newValue = new ComponentNewValue("uuid", "name", "key", true, "path", "APP");

    assertThat(newValue.toString()).contains("applicationUuid");
  }

}

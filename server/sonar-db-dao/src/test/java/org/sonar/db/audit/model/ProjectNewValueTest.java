/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectNewValueTest {

  @Test
  void toString_generatesValidJson() throws ParseException {
    var project = new ProjectDto()
      .setUuid("uuid")
      .setName("name")
      .setKey("key")
      .setPrivate(true)
      .setDescription("description")
      .setQualifier("TRK")
      .setAiCodeFixEnabled(true);
    ProjectNewValue newValue = new ProjectNewValue(project);

    JSONObject jsonObject = (JSONObject) new JSONParser().parse(newValue.toString());

    assertThat(jsonObject).hasSize(7);
  }
}

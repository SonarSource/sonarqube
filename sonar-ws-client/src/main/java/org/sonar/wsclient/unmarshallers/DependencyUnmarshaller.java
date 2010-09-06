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
package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONObject;
import org.sonar.wsclient.services.Dependency;

public class DependencyUnmarshaller extends AbstractUnmarshaller<Dependency> {

  @Override
  protected Dependency parse(JSONObject json) {
    return new Dependency()
        .setId(JsonUtils.getString(json, "id"))
        .setFromId(JsonUtils.getLong(json, "fi"))
        .setToId(JsonUtils.getLong(json, "ti"))
        .setFromKey(JsonUtils.getString(json, "fk"))
        .setToKey(JsonUtils.getString(json, "tk"))
        .setUsage(JsonUtils.getString(json, "u"))
        .setWeight(JsonUtils.getInteger(json, "w"))
        .setFromName(JsonUtils.getString(json, "fn"))
        .setFromQualifier(JsonUtils.getString(json, "fq"))
        .setToName(JsonUtils.getString(json, "tn"))
        .setToQualifier(JsonUtils.getString(json, "tq"));
  }
}
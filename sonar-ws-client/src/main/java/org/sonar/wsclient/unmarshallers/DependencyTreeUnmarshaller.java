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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sonar.wsclient.services.DependencyTree;

import java.util.ArrayList;
import java.util.List;

public class DependencyTreeUnmarshaller extends AbstractUnmarshaller<DependencyTree> {
  @Override
  protected DependencyTree parse(JSONObject json) {
    DependencyTree tree = new DependencyTree()
        .setDepId(JsonUtils.getString(json, "did"))
        .setResourceId(JsonUtils.getString(json, "rid"))
        .setResourceKey(JsonUtils.getString(json, "k"))
        .setResourceName(JsonUtils.getString(json, "n"))
        .setResourceScope(JsonUtils.getString(json, "s"))
        .setResourceQualifier(JsonUtils.getString(json, "q"))
        .setResourceVersion(JsonUtils.getString(json, "v"))
        .setUsage(JsonUtils.getString(json, "u"))
        .setWeight(JsonUtils.getInteger(json, "w"));

    List<DependencyTree> to = new ArrayList<DependencyTree>();
    tree.setTo(to);

    JSONArray toJson = (JSONArray) json.get("to");
    if (toJson != null) {
      for (Object aToJson : toJson) {
        to.add(parse((JSONObject) aToJson));
      }
    }
    return tree;
  }
}

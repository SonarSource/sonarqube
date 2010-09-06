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
import org.json.simple.JSONValue;
import org.sonar.wsclient.services.Model;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUnmarshaller<MODEL extends Model> implements Unmarshaller<MODEL> {

  public final MODEL toModel(String json) {
    MODEL  result = null;
    JSONArray array = (JSONArray) JSONValue.parse(json);
    if (array.size() >= 1) {
      JSONObject elt = (JSONObject) array.get(0);
      if (elt != null) {
        result = parse(elt);
      }
    }
    return result;

  }

  public final List<MODEL> toModels(String json) {
    List<MODEL> result = new ArrayList<MODEL>();
    JSONArray array = (JSONArray) JSONValue.parse(json);
    for (Object anArray : array) {
      JSONObject elt = (JSONObject) anArray;
      if (elt != null) {
        result.add(parse(elt));
      }
    }
    return result;
  }

  protected abstract MODEL parse(JSONObject elt);
}

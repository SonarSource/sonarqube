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
package org.sonar.wsclient.gwt.unmarshallers;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.sonar.wsclient.services.Model;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUnmarshaller<MODEL extends Model> implements Unmarshaller<MODEL> {
  public final MODEL toModel(JavaScriptObject json) {
    JSONArray array = new JSONArray(json);
    if (array.size() >= 1) {
      JSONObject elt = array.get(0).isObject();
      return parse(elt);
    }
    return null;
  }

  public final List<MODEL> toModels(JavaScriptObject json) {
    List<MODEL> result = new ArrayList<MODEL>();
    JSONArray array = new JSONArray(json);
    for (int i = 0; i < array.size(); i++) {
      JSONObject elt = array.get(i).isObject();
      if (elt != null) {
        result.add(parse(elt));
      }
    }
    return result;
  }

  protected abstract MODEL parse(JSONObject json);
}

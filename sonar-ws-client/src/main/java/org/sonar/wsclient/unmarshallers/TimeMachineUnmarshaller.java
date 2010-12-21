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
import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineColumn;

public class TimeMachineUnmarshaller extends AbstractUnmarshaller<TimeMachine> {

  protected TimeMachine parse(JSONObject json) {
    JSONArray cols = JsonUtils.getArray(json, "cols");
    JSONArray cells = JsonUtils.getArray(json, "cells");
    return new TimeMachine(toColumns(cols), toCells(cells));
  }

  private TimeMachineColumn[] toColumns(JSONArray cols) {
    int size = cols.size();
    TimeMachineColumn[] result = new TimeMachineColumn[size];
    for (int index = 0; index < size; index++) {
      JSONObject colJson = (JSONObject) cols.get(index);
      result[index] = new TimeMachineColumn(index, JsonUtils.getString(colJson, "metric"), null, null);
    }
    return result;
  }

  private TimeMachineCell[] toCells(JSONArray cells) {
    int size = cells.size();
    TimeMachineCell[] result = new TimeMachineCell[size];
    for (int i = 0; i < size; i++) {
      JSONObject cellJson = (JSONObject) cells.get(i);
      JSONArray valuesJson = JsonUtils.getArray(cellJson, "v");

      Object[] resultValues = new Object[valuesJson.size()];
      for (int indexValue = 0; indexValue < valuesJson.size(); indexValue++) {
        Object value = valuesJson.get(indexValue);
        resultValues[indexValue] = value;
      }
      result[i] = new TimeMachineCell(JsonUtils.getDateTime(cellJson, "d"), resultValues);
    }
    return result;
  }

}

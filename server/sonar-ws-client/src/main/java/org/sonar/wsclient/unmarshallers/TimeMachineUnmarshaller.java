/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineColumn;
import org.sonar.wsclient.services.WSUtils;

public class TimeMachineUnmarshaller extends AbstractUnmarshaller<TimeMachine> {

  @Override
  protected TimeMachine parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    Object cols = utils.getField(json, "cols");
    if (cols == null) {
      throw new IllegalArgumentException("cols must be set");
    }
    Object cells = utils.getField(json, "cells");
    if (cells == null) {
      throw new IllegalArgumentException("cells must be set");
    }
    return new TimeMachine(toColumns(cols), toCells(cells));
  }

  private TimeMachineColumn[] toColumns(Object cols) {
    WSUtils utils = WSUtils.getINSTANCE();
    int size = utils.getArraySize(cols);
    TimeMachineColumn[] result = new TimeMachineColumn[size];
    for (int index = 0; index < size; index++) {
      Object colJson = utils.getArrayElement(cols, index);
      result[index] = new TimeMachineColumn(index, utils.getString(colJson, "metric"), null, null);
    }
    return result;
  }

  private TimeMachineCell[] toCells(Object cells) {
    WSUtils utils = WSUtils.getINSTANCE();
    int size = utils.getArraySize(cells);
    TimeMachineCell[] result = new TimeMachineCell[size];
    for (int i = 0; i < size; i++) {
      Object cellJson = utils.getArrayElement(cells, i);
      Object valuesJson = utils.getField(cellJson, "v");
      if (valuesJson != null) {
        Object[] resultValues = new Object[utils.getArraySize(valuesJson)];
        for (int indexValue = 0; indexValue < utils.getArraySize(valuesJson); indexValue++) {
          Object value = utils.getArrayElement(valuesJson, indexValue);
          resultValues[indexValue] = value;
        }
        result[i] = new TimeMachineCell(utils.getDateTime(cellJson, "d"), resultValues);
      }
    }
    return result;
  }

}

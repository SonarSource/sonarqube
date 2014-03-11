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
package org.sonar.wsclient.services;

/**
 * Past values of a given resource
 *
 * @since 2.5
 */
public class TimeMachine extends Model {

  private TimeMachineColumn[] columns;
  private TimeMachineCell[] cells;

  public TimeMachine(TimeMachineColumn[] columns, TimeMachineCell[] cells) {
    this.columns = columns;
    this.cells = cells;
  }

  public TimeMachineColumn[] getColumns() {
    return columns;
  }

  public TimeMachineCell[] getCells() {
    return cells;
  }

  public TimeMachineColumn getColumn(String metricKey) {
    for (TimeMachineColumn column : columns) {
      if (metricKey.equals(column.getMetricKey()) && column.getCharacteristicKey()==null) {
        return column;
      }
    }
    return null;
  }

  public int getColumnIndex(String metricKey) {
    TimeMachineColumn col = getColumn(metricKey);
    return col!=null ? col.getIndex() : -1;
  }
}

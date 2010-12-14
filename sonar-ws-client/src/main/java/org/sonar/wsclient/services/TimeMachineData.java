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
package org.sonar.wsclient.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @since 2.5
 */
public class TimeMachineData extends Model {
  private Date date;

  /**
   * We use strings here in order to support measures with string value.
   */
  private List<String> values = new ArrayList<String>();

  public Date getDate() {
    return date;
  }

  public TimeMachineData setDate(Date date) {
    this.date = date;
    return this;
  }

  public List<String> getValues() {
    return values;
  }

  public TimeMachineData setValues(List<String> values) {
    this.values = values;
    return this;
  }

  public Double getValueAsDouble(int index) {
    String valueStr = values.get(index);
    try {
      return valueStr == null ? null : Double.valueOf(valueStr);
    } catch (NumberFormatException e) {
      return null;
    }
  }

}

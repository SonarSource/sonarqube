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

package org.sonar.core.measure.db;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.Serializable;

public class MeasureKey implements Serializable {

  private final String componentKey;
  private final String metricKey;

  private MeasureKey(String componentKey, String metricKey) {
    this.componentKey = componentKey;
    this.metricKey = metricKey;
  }

  public String componentKey() {
    return componentKey;
  }

  public String metricKey() {
    return metricKey;
  }

  public static MeasureKey of(String componentKey, String metricKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(componentKey), "Component key must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(metricKey), "Metric key must be set");
    return new MeasureKey(componentKey, metricKey);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MeasureKey that = (MeasureKey) o;

    if (!componentKey.equals(that.componentKey)) {
      return false;
    }
    if (!metricKey.equals(that.metricKey)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = componentKey.hashCode();
    result = 31 * result + metricKey.hashCode();
    return result;
  }
}

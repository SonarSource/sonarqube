/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.measures;

import java.util.IdentityHashMap;
import java.util.Map;

public class Measures {

  private Map<MetricDef, Measure> measures = new IdentityHashMap<MetricDef, Measure>();

  public double getValue(MetricDef metric) {
    Measure measure = measures.get(metric);
    if (measure == null) {
      return 0;
    }
    return measure.getValue();
  }

  public Object getData(MetricDef metric) {
    Measure measure = measures.get(metric);
    if (measure == null) {
      return null;
    }
    return measure.getData();
  }

  public void setValue(MetricDef metric, double measure) {
    getMeasureOrCreateIt(metric).setValue(measure);
  }

  public void setData(MetricDef metric, Object data) {
    getMeasureOrCreateIt(metric).setData(data);
  }

  private Measure getMeasureOrCreateIt(MetricDef metric) {
    Measure measure = measures.get(metric);
    if (measure == null) {
      measure = new Measure(0);
      measures.put(metric, measure);
    }
    return measure;
  }

  public void removeMeasure(MetricDef metric) {
    measures.remove(metric);
  }

  private static final class Measure {

    private double value;
    private Object data;

    private Measure(double value) {
      this.value = value;
    }

    private double getValue() {
      return value;
    }

    private void setValue(double value) {
      this.value = value;
    }

    private Object getData() {
      return data;
    }

    private void setData(Object data) {
      this.data = data;
    }
  }

}

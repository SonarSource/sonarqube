/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.test;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;

public class IsMeasure extends BaseMatcher<Measure> {

  private Metric metric = null;
  private Double value = null;
  private String data = null;
  private String mismatchTxt;

  public IsMeasure(Metric metric, Double value, String data) {
    this.metric = metric;
    this.value = value;
    this.data = data;
  }

  public IsMeasure(Metric metric) {
    this.metric = metric;
  }

  public IsMeasure(Metric metric, Double value) {
    this.metric = metric;
    this.value = value;
  }

  public IsMeasure(Metric metric, String data) {
    this.metric = metric;
    this.data = data;
  }

  public boolean matches(Object o) {
    Measure m = (Measure) o;
    if (metric != null && !ObjectUtils.equals(metric, m.getMetric())) {
      mismatchTxt = "metric: " + metric.getKey();
      return false;
    }

    if (value != null && NumberUtils.compare(value, m.getValue()) != 0) {
      mismatchTxt = "value: " + value;
      return false;
    }

    if (data != null && !ObjectUtils.equals(data, m.getData())) {
      mismatchTxt = "data: " + data;
      return false;
    }
    return true;
  }

  public void describeTo(Description description) {
    description.appendText(mismatchTxt);
  }
}

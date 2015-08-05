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

package org.sonar.api.test.ce.measure;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Measure;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@Immutable
public class MeasureImpl implements Measure {

  private Integer intValue;
  private Long longValue;
  private Double doubleValue;
  private String stringValue;

  public static MeasureImpl createMeasure(double doubleValue){
    MeasureImpl measure = new MeasureImpl();
    measure.doubleValue = doubleValue;
    return measure;
  }

  public static MeasureImpl createMeasure(int intValue) {
    MeasureImpl measure = new MeasureImpl();
    measure.intValue = intValue;
    return measure;
  }

  public static MeasureImpl createMeasure(long longValue) {
    MeasureImpl measure = new MeasureImpl();
    measure.longValue = longValue;
    return measure;
  }

  public static MeasureImpl createMeasure(String stringValue) {
    MeasureImpl measure = new MeasureImpl();
    measure.stringValue = requireNonNull(stringValue, "Value cannot be null");
    return measure;
  }

  @Override
  public int getIntValue() {
    checkState(intValue != null, "Not an int measure");
    return intValue;
  }

  @Override
  public long getLongValue() {
    checkState(longValue != null, "Not a long measure");
    return longValue;
  }

  @Override
  public double getDoubleValue() {
    checkState(doubleValue != null, "Not a double measure");
    return doubleValue;
  }

  @Override
  public String getStringValue() {
    checkState(stringValue != null, "Not a string measure");
    return stringValue;
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.ce.measure.test;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Measure;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@Immutable
public class TestMeasure implements Measure {

  private Integer intValue;
  private Long longValue;
  private Double doubleValue;
  private String stringValue;
  private Boolean booleanValue;

  public static TestMeasure createMeasure(double doubleValue){
    TestMeasure measure = new TestMeasure();
    measure.doubleValue = doubleValue;
    return measure;
  }

  public static TestMeasure createMeasure(int intValue) {
    TestMeasure measure = new TestMeasure();
    measure.intValue = intValue;
    return measure;
  }

  public static TestMeasure createMeasure(long longValue) {
    TestMeasure measure = new TestMeasure();
    measure.longValue = longValue;
    return measure;
  }

  public static TestMeasure createMeasure(String stringValue) {
    TestMeasure measure = new TestMeasure();
    measure.stringValue = requireNonNull(stringValue, "Value cannot be null");
    return measure;
  }

  public static TestMeasure createMeasure(boolean booleanValue) {
    TestMeasure measure = new TestMeasure();
    measure.booleanValue = requireNonNull(booleanValue, "Value cannot be null");
    return measure;
  }

  @Override
  public int getIntValue() {
    checkState(intValue != null, "Not an integer measure");
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

  @Override
  public boolean getBooleanValue() {
    checkState(booleanValue != null, "Not a boolean measure");
    return booleanValue;
  }
}

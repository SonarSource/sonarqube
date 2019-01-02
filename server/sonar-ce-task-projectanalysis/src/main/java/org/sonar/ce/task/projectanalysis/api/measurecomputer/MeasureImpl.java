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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import java.util.EnumSet;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Measure;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.measure.Measure.ValueType.BOOLEAN;
import static org.sonar.ce.task.projectanalysis.measure.Measure.ValueType.DOUBLE;
import static org.sonar.ce.task.projectanalysis.measure.Measure.ValueType.INT;
import static org.sonar.ce.task.projectanalysis.measure.Measure.ValueType.LONG;
import static org.sonar.ce.task.projectanalysis.measure.Measure.ValueType.STRING;

@Immutable
public class MeasureImpl implements Measure {

  private static final EnumSet<org.sonar.ce.task.projectanalysis.measure.Measure.ValueType> ALLOWED_VALUE_TYPES = EnumSet.of(INT, LONG, DOUBLE, STRING, BOOLEAN);

  private final org.sonar.ce.task.projectanalysis.measure.Measure measure;

  public MeasureImpl(org.sonar.ce.task.projectanalysis.measure.Measure measure) {
    this.measure = requireNonNull(measure, "Measure couldn't be null");
    checkState(ALLOWED_VALUE_TYPES.contains(measure.getValueType()), "Only following types are allowed %s", ALLOWED_VALUE_TYPES);
  }

  @Override
  public int getIntValue() {
    checkValueType(INT);
    return measure.getIntValue();
  }

  @Override
  public long getLongValue() {
    checkValueType(LONG);
    return measure.getLongValue();
  }

  @Override
  public double getDoubleValue() {
    checkValueType(DOUBLE);
    return measure.getDoubleValue();
  }

  @Override
  public String getStringValue() {
    checkValueType(STRING);
    return measure.getStringValue();
  }

  @Override
  public boolean getBooleanValue() {
    checkValueType(BOOLEAN);
    return measure.getBooleanValue();
  }

  private void checkValueType(org.sonar.ce.task.projectanalysis.measure.Measure.ValueType expected) {
    if (measure.getValueType() != expected) {
      throw new IllegalStateException(format("Value can not be converted to %s because current value type is a %s",
        expected.toString().toLowerCase(Locale.US),
        measure.getValueType()));
    }
  }

  @Override
  public String toString() {
    return "MeasureImpl{" +
      "measure=" + measure +
      '}';
  }
}

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

package org.sonar.server.computation.measure.api;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Measure;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.measure.Measure.ValueType.BOOLEAN;
import static org.sonar.server.computation.measure.Measure.ValueType.DOUBLE;
import static org.sonar.server.computation.measure.Measure.ValueType.INT;
import static org.sonar.server.computation.measure.Measure.ValueType.LONG;
import static org.sonar.server.computation.measure.Measure.ValueType.STRING;

@Immutable
public class MeasureImpl implements Measure {

  private static final Set<org.sonar.server.computation.measure.Measure.ValueType> ALLOWED_VALUE_TYPES = ImmutableSet.of(INT, LONG, DOUBLE, STRING, BOOLEAN);

  private final org.sonar.server.computation.measure.Measure measure;

  public MeasureImpl(org.sonar.server.computation.measure.Measure measure) {
    this.measure = requireNonNull(measure, "Measure couldn't be null");
    checkState(ALLOWED_VALUE_TYPES.contains(measure.getValueType()), String.format("Only following types are allowed %s", ALLOWED_VALUE_TYPES));
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

  private void checkValueType(org.sonar.server.computation.measure.Measure.ValueType expected) {
    checkState(measure.getValueType() == expected, String.format(
      "Value can not be converted to %s because current value type is a %s",
      expected.toString().toLowerCase(Locale.US),
      measure.getValueType()
      ));
  }

  @Override
  public String toString() {
    return "MeasureImpl{" +
      "measure=" + measure +
      '}';
  }
}

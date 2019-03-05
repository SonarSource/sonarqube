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
package org.sonar.api.batch.fs.internal;

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.measure.Metric;

/**
 * @since 5.2
 */
public abstract class DefaultInputComponent implements InputComponent {
  private int id;
  private Set<String> storedMetricKeys = new HashSet<>();

  public DefaultInputComponent(int scannerId) {
    this.id = scannerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    DefaultInputComponent that = (DefaultInputComponent) o;
    return key().equals(that.key());
  }

  public int scannerId() {
    return id;
  }

  @Override
  public int hashCode() {
    return key().hashCode();
  }

  @Override
  public String toString() {
    return "[key=" + key() + "]";
  }

  public void setHasMeasureFor(Metric metric) {
    storedMetricKeys.add(metric.key());
  }

  public boolean hasMeasureFor(Metric metric) {
    return storedMetricKeys.contains(metric.key());
  }
}

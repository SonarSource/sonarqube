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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.ce.task.projectanalysis.component.Developer;
import org.sonar.ce.task.projectanalysis.component.Developer;

import static java.util.Objects.requireNonNull;

@Immutable
public final class MeasureKey {

  private final String metricKey;
  @CheckForNull
  private final Developer developer;

  public MeasureKey(String metricKey, @Nullable Developer developer) {
    this.metricKey = requireNonNull(metricKey, "MetricKey can not be null");
    this.developer = developer;
  }

  public String getMetricKey() {
    return metricKey;
  }

  @CheckForNull
  public Developer getDeveloper() {
    return developer;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MeasureKey that = (MeasureKey) o;
    return metricKey.equals(that.metricKey)
      && developer == that.developer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricKey);
  }

  @Override
  public String toString() {
    return "MeasureKey{" +
      "metricKey='" + metricKey + '\'' +
      ", developer=" + developer +
      '}';
  }
}

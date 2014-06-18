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
package org.sonar.api.batch.analyzer.measure.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.fs.InputFile;

import javax.annotation.Nullable;

import java.io.Serializable;

public class DefaultAnalyzerMeasure<G extends Serializable> implements AnalyzerMeasure<G>, Serializable {

  private final InputFile inputFile;
  private final String metricKey;
  private final G value;

  DefaultAnalyzerMeasure(DefaultAnalyzerMeasureBuilder<G> builder) {
    Preconditions.checkNotNull(builder.value, "Measure value can't be null");
    Preconditions.checkNotNull(builder.metricKey, "Measure metricKey can't be null");
    this.inputFile = builder.file;
    this.metricKey = builder.metricKey;
    this.value = builder.value;
  }

  @Nullable
  @Override
  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public String metricKey() {
    return metricKey;
  }

  @Override
  public G value() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DefaultAnalyzerMeasure)) {
      return false;
    }
    DefaultAnalyzerMeasure<?> other = (DefaultAnalyzerMeasure<?>) obj;
    return metricKey.equals(other.metricKey)
      && value.equals(other.value)
      && (inputFile == null ? other.inputFile == null : inputFile.equals(other.inputFile));
  }

  @Override
  public int hashCode() {
    return metricKey.hashCode()
      + value.hashCode()
      + (inputFile != null ? inputFile.hashCode() : 0);
  }

  @Override
  public String toString() {
    return "AnalyzerMeasure[" + (inputFile != null ? "inputFile=" + inputFile.toString() : "onProject")
      + ",metricKey=" + metricKey + ",value=" + value + "]";
  }

}

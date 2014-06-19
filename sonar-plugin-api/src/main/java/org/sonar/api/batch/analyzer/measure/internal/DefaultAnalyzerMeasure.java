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

import org.sonar.api.batch.measure.Metric;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.fs.InputFile;

import javax.annotation.Nullable;

import java.io.Serializable;

public class DefaultAnalyzerMeasure<G extends Serializable> implements AnalyzerMeasure<G>, Serializable {

  private final InputFile inputFile;
  private final Metric<G> metric;
  private final G value;

  DefaultAnalyzerMeasure(DefaultAnalyzerMeasureBuilder<G> builder) {
    Preconditions.checkNotNull(builder.value, "Measure value can't be null");
    Preconditions.checkNotNull(builder.metric, "Measure metric can't be null");
    Preconditions.checkState(builder.metric.valueType().equals(builder.value.getClass()), "Measure value should be of type " + builder.metric.valueType());
    this.inputFile = builder.file;
    this.metric = builder.metric;
    this.value = builder.value;
  }

  @Nullable
  @Override
  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public Metric<G> metric() {
    return metric;
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
    return metric.equals(other.metric)
      && value.equals(other.value)
      && (inputFile == null ? other.inputFile == null : inputFile.equals(other.inputFile));
  }

  @Override
  public int hashCode() {
    return metric.hashCode()
      + value.hashCode()
      + (inputFile != null ? inputFile.hashCode() : 0);
  }

  @Override
  public String toString() {
    return "AnalyzerMeasure[" + (inputFile != null ? "inputFile=" + inputFile.toString() : "onProject")
      + ",metric=" + metric + ",value=" + value + "]";
  }

}

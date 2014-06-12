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
package org.sonar.batch.api.analyzer.measure;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.api.internal.Preconditions;
import org.sonar.batch.api.measures.Metric;

import javax.annotation.Nullable;

import java.io.Serializable;

public class AnalyzerMeasure<G extends Serializable> implements Serializable {

  private final InputFile inputFile;
  private final String metricKey;
  private final G value;

  private AnalyzerMeasure(Builder<G> builder) {
    Preconditions.checkNotNull(builder.value, "Measure value can't be null");
    Preconditions.checkNotNull(builder.metricKey, "Measure metricKey can't be null");
    this.inputFile = builder.file;
    this.metricKey = builder.metricKey;
    this.value = builder.value;
  }

  @Nullable
  public InputFile inputFile() {
    return inputFile;
  }

  public String metricKey() {
    return metricKey;
  }

  public Serializable value() {
    return value;
  }

  public static <G extends Serializable> Builder<G> builder() {
    return new Builder<G>();
  }

  public static class Builder<G extends Serializable> {

    private Boolean onProject = null;
    private InputFile file;
    private String metricKey;
    private G value;

    public Builder<G> onFile(InputFile file) {
      Preconditions.checkState(onProject == null, "onFile or onProject can be called only once");
      Preconditions.checkNotNull(file, "InputFile should be non null");
      this.file = file;
      this.onProject = false;
      return this;
    }

    public Builder<G> onProject() {
      Preconditions.checkState(onProject == null, "onFile or onProject can be called only once");
      this.file = null;
      this.onProject = true;
      return this;
    }

    private Builder<G> metricKey(String metricKey) {
      Preconditions.checkState(metricKey != null, "Metric already defined");
      this.metricKey = metricKey;
      return this;
    }

    public Builder<G> forMetric(Metric<G> metric) {
      return metricKey(metric.key());
    }

    public Builder<G> withValue(G value) {
      Preconditions.checkState(value != null, "Measure value already defined");
      Preconditions.checkNotNull(value, "Measure value can't be null");
      this.value = value;
      return this;
    }

    public AnalyzerMeasure<G> build() {
      return new AnalyzerMeasure<G>(this);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AnalyzerMeasure)) {
      return false;
    }
    AnalyzerMeasure<?> other = (AnalyzerMeasure<?>) obj;
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

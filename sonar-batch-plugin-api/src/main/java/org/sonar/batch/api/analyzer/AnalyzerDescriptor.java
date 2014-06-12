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
package org.sonar.batch.api.analyzer;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.api.measures.Metric;

import java.util.Arrays;
import java.util.Collection;

public class AnalyzerDescriptor {

  private final String name;
  private final Metric<?>[] dependsOn;
  private final Metric<?>[] provides;
  private final String[] languages;
  private final InputFile.Type[] types;

  private AnalyzerDescriptor(Builder builder) {
    this.name = builder.name;
    this.dependsOn = builder.dependsOn != null ? builder.dependsOn : new Metric<?>[0];
    this.provides = builder.provides != null ? builder.provides : new Metric<?>[0];
    this.languages = builder.languages != null ? builder.languages : new String[0];
    this.types = builder.types;
  }

  public String name() {
    return name;
  }

  public Metric<?>[] dependsOn() {
    return dependsOn;
  }

  public Metric<?>[] provides() {
    return provides;
  }

  public Collection<String> languages() {
    return Arrays.asList(languages);
  }

  public InputFile.Type[] types() {
    return types;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private Metric<?>[] dependsOn;
    private Metric<?>[] provides;
    private String[] languages;
    private InputFile.Type[] types;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder dependsOn(Metric<?>... metrics) {
      this.dependsOn = metrics;
      return this;
    }

    public Builder provides(Metric<?>... metrics) {
      this.provides = metrics;
      return this;
    }

    public Builder runOnLanguages(String... languageKeys) {
      this.languages = languageKeys;
      return this;
    }

    public Builder runOnTypes(InputFile.Type... types) {
      this.types = types;
      return this;
    }

    public AnalyzerDescriptor build() {
      return new AnalyzerDescriptor(this);
    }

  }

}

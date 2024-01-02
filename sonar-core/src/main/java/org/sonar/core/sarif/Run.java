/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Run {

  @SerializedName("tool")
  private final Tool tool;
  @SerializedName("results")
  private final Set<Result> results;
  @SerializedName("language")
  private final String language;
  @SerializedName("columnKind")
  private final String columnKind;

  private Run(Tool tool, Set<Result> results, @Nullable String language, @Nullable String columnKind) {
    this.tool = tool;
    this.results = Set.copyOf(results);
    this.language = language;
    this.columnKind = columnKind;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  @CheckForNull
  public String getColumnKind() {
    return columnKind;
  }

  public Tool getTool() {
    return tool;
  }

  public Set<Result> getResults() {
    return results;
  }

  public static RunBuilder builder() {
    return new RunBuilder();
  }

  public static final class RunBuilder {
    private Tool tool;
    private Set<Result> results;
    private String language;
    private String columnKind;

    private RunBuilder() {
    }

    public RunBuilder tool(Tool tool) {
      this.tool = tool;
      return this;
    }

    public RunBuilder results(Set<Result> results) {
      this.results = results;
      return this;
    }

    public RunBuilder language(String language) {
      this.language = language;
      return this;
    }

    public RunBuilder columnKind(String columnKind) {
      this.columnKind = columnKind;
      return this;
    }

    public Run build() {
      return new Run(tool, results, language, columnKind);
    }
  }

}

/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import java.util.Set;

public class Run {

  @VisibleForTesting
  public static final String LANGUAGE_EN_US = "en-us";
  @VisibleForTesting
  public static final String COLUMN_KIND = "utf16CodeUnits";

  @SerializedName("tool")
  private final Tool tool;
  @SerializedName("results")
  private final Set<Result> results;
  @SerializedName("language")
  private final String language;
  @SerializedName("columnKind")
  private final String columnKind;

  public Run(Tool tool, Set<Result> results) {
    this(tool, results, LANGUAGE_EN_US, COLUMN_KIND);
  }

  private Run(Tool tool, Set<Result> results, String language, String columnKind) {
    this.tool = tool;
    this.results = Set.copyOf(results);
    this.language = language;
    this.columnKind = columnKind;
  }

  public String getLanguage() {
    return language;
  }

  public String getColumnKind() {
    return columnKind;
  }

  public Tool getTool() {
    return tool;
  }

  public Set<Result> getResults() {
    return results;
  }
}

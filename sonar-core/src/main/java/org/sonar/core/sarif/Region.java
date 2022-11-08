/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class Region {
  @SerializedName("startLine")
  private final int startLine;
  @SerializedName("endLine")
  private final int endLine;
  @SerializedName("startColumn")
  private final int startColumn;
  @SerializedName("endColumn")
  private final int endColumn;

  private Region(int startLine, int endLine, int startColumn, int endColumn) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }

  public static RegionBuilder builder() {
    return new RegionBuilder();
  }

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public static final class RegionBuilder {
    private int startLine;
    private int endLine;
    private int startColumn;
    private int endColumn;

    public RegionBuilder startLine(int startLine) {
      this.startLine = startLine;
      return this;
    }

    public RegionBuilder endLine(int endLine) {
      this.endLine = endLine;
      return this;
    }

    public RegionBuilder startColumn(int startColumn) {
      this.startColumn = startColumn;
      return this;
    }

    public RegionBuilder endColumn(int endColumn) {
      this.endColumn = endColumn;
      return this;
    }

    public Region build() {
      return new Region(startLine, endLine, startColumn, endColumn);
    }
  }
}

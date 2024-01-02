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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Region {
  @SerializedName("startLine")
  private final Integer startLine;
  @SerializedName("endLine")
  private final Integer endLine;
  @SerializedName("startColumn")
  private final Integer startColumn;
  @SerializedName("endColumn")
  private final Integer endColumn;

  private Region(Integer startLine, @Nullable Integer endLine, @Nullable Integer startColumn, @Nullable Integer endColumn) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }

  public static RegionBuilder builder() {
    return new RegionBuilder();
  }

  public Integer getStartLine() {
    return startLine;
  }

  @CheckForNull
  public Integer getEndLine() {
    return endLine;
  }

  @CheckForNull
  public Integer getStartColumn() {
    return startColumn;
  }

  @CheckForNull
  public Integer getEndColumn() {
    return endColumn;
  }

  public static final class RegionBuilder {
    private Integer startLine;
    private Integer endLine;
    private Integer startColumn;
    private Integer endColumn;

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

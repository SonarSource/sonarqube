/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.model;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import org.jetbrains.annotations.Nullable;

public record RestPage(
  @Min(1)
  @Max(500)
  @Parameter(
    description = "Number of results per page",
    schema = @Schema(defaultValue = DEFAULT_PAGE_SIZE, implementation = Integer.class))
  Integer pageSize,

  @Positive
  @Parameter(
    description = "1-based page number",
    schema = @Schema(defaultValue = DEFAULT_PAGE_INDEX, implementation = Integer.class))
  Integer pageIndex
) {

  @VisibleForTesting
  public static final String DEFAULT_PAGE_SIZE = "50";
  @VisibleForTesting
  public static final String DEFAULT_PAGE_INDEX = "1";

  public RestPage(@Nullable Integer pageSize, @Nullable Integer pageIndex) {
    this.pageSize = pageSize == null ? Integer.valueOf(DEFAULT_PAGE_SIZE) : pageSize;
    this.pageIndex = pageIndex == null ? Integer.valueOf(DEFAULT_PAGE_INDEX) : pageIndex;
  }

}

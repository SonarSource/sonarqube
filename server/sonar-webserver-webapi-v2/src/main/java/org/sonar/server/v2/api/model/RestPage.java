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
package org.sonar.server.v2.api.model;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.jetbrains.annotations.Nullable;

public record RestPage(
  @PositiveOrZero
  @Max(500)
  @Schema(defaultValue = DEFAULT_PAGE_SIZE, description = "Number of results per page. A value of 0 will only return the pagination information.")
  Integer pageSize,

  @Positive
  @Min(1)
  @Schema(defaultValue = DEFAULT_PAGE_INDEX, description = "1-based page index")
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

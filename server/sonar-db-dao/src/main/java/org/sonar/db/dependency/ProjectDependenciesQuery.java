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
package org.sonar.db.dependency;

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.db.DaoUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;

public final class ProjectDependenciesQuery {
  private final String branchUuid;
  @Nullable
  private final String query;

  public ProjectDependenciesQuery(String branchUuid, @Nullable String query) {
    this.branchUuid = branchUuid;
    this.query = query;
  }

  /**
   * Used by MyBatis mapper
   */
  @CheckForNull
  public String getLikeQuery() {
    return query == null ? null : buildLikeValue(query, BEFORE_AND_AFTER).toLowerCase(Locale.ENGLISH);
  }

  public String branchUuid() {
    return branchUuid;
  }

  @Nullable
  public String query() {
    return query;
  }


}

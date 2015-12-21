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

package org.sonar.db.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DatabaseUtils.buildLikeValue;
import static org.sonar.db.WildcardPosition.AFTER;

public class ComponentQuery {
  private final String nameOrKeyQuery;
  private final String[] qualifiers;
  private final String language;

  public ComponentQuery(@Nullable String nameOrKeyQuery, String language, String... qualifiers) {
    this.language = language;
    checkArgument(qualifiers.length > 0, "At least one qualifier must be provided");

    this.nameOrKeyQuery = nameOrKeyQuery;
    this.qualifiers = qualifiers;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  @CheckForNull
  public String getNameOrKeyQuery() {
    return nameOrKeyQuery;
  }

  @CheckForNull
  public String getNameOrKeyQueryToSqlForResourceIndex() {
    return buildLikeValue(nameOrKeyQuery, AFTER).toLowerCase();
  }

  @CheckForNull
  public String getNameOrKeyQueryToSqlForProjectKey() {
    return buildLikeValue(nameOrKeyQuery, AFTER);
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }
}
